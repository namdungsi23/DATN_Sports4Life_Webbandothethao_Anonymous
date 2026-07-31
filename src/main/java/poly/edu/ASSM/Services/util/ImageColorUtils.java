package poly.edu.ASSM.Services.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/**
 * Nhận diện tên màu tiếng Việt từ tên file, hex Cloudinary, hoặc ảnh.
 */
public final class ImageColorUtils {

	public record NamedColor(String name, int r, int g, int b) {
	}

	private static final NamedColor[] PALETTE = {
			new NamedColor("Đen", 25, 25, 25),
			new NamedColor("Trắng", 245, 245, 245),
			new NamedColor("Xám", 140, 140, 140),
			new NamedColor("Xám đậm", 80, 80, 80),
			new NamedColor("Đỏ", 200, 40, 40),
			new NamedColor("Hồng", 230, 120, 160),
			new NamedColor("Cam", 230, 120, 40),
			new NamedColor("Vàng", 230, 200, 50),
			new NamedColor("Be", 210, 190, 150),
			new NamedColor("Nâu", 120, 75, 45),
			new NamedColor("Xanh Navy", 25, 45, 95),
			new NamedColor("Xanh Dương", 40, 100, 200),
			new NamedColor("Xanh Lá", 45, 140, 70),
			new NamedColor("Tím", 120, 60, 160),
			new NamedColor("Bạc", 192, 192, 192),
	};

	private static final Map<Pattern, String> FILENAME_COLORS = new LinkedHashMap<>();

	static {
		putFile("đen|den|black|blk", "Đen");
		putFile("trắng|trang|white|wht", "Trắng");
		putFile("xám\\s*đậm|xam\\s*dam|dark\\s*gr[ae]y|charcoal", "Xám đậm");
		putFile("xám|xam|gr[ae]y|grey", "Xám");
		putFile("đỏ|do(?![a-z])|red", "Đỏ");
		putFile("hồng|hong|pink", "Hồng");
		putFile("cam|orange", "Cam");
		putFile("vàng|vang|yellow|gold", "Vàng");
		putFile("be|beige|cream", "Be");
		putFile("nâu|nau|brown", "Nâu");
		putFile("navy|xanh\\s*navy|xanh\\s*đen", "Xanh Navy");
		putFile("xanh\\s*d[uươ]ơ?ng|blue(?!\\s*navy)|blu", "Xanh Dương");
		putFile("xanh\\s*lá|xanh\\s*la|green|grn", "Xanh Lá");
		putFile("tím|tim|purple|violet", "Tím");
		putFile("bạc|bac|silver|slv", "Bạc");
	}

	private static void putFile(String regex, String name) {
		FILENAME_COLORS.put(
				Pattern.compile("(?:^|[_\\-\\s/])(?:" + regex + ")(?:$|[_\\-\\s./])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE),
				name);
	}

	private ImageColorUtils() {
	}

	public static boolean isPlaceholder(String color) {
		if (color == null || color.isBlank()) {
			return true;
		}
		String c = color.trim().toLowerCase(Locale.ROOT);
		// "Free" là size phụ kiện, không phải màu placeholder
		return c.equals("default") || c.equals("mặc định") || c.equals("mac dinh");
	}

	public static Optional<String> fromFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			return Optional.empty();
		}
		String base = filename;
		int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
		if (slash >= 0) {
			base = base.substring(slash + 1);
		}
		int dot = base.lastIndexOf('.');
		if (dot > 0) {
			base = base.substring(0, dot);
		}
		String normalized = base.replace('đ', 'd').replace('Đ', 'D');
		for (Map.Entry<Pattern, String> e : FILENAME_COLORS.entrySet()) {
			if (e.getKey().matcher(base).find() || e.getKey().matcher(normalized).find()) {
				return Optional.of(e.getValue());
			}
		}
		return Optional.empty();
	}

	public static String fromHex(String hex) {
		int[] rgb = parseHex(hex);
		if (rgb == null) {
			return "Đen";
		}
		return nearestName(rgb[0], rgb[1], rgb[2]);
	}

	public static String nearestName(int r, int g, int b) {
		NamedColor best = PALETTE[0];
		double bestDist = Double.MAX_VALUE;
		for (NamedColor c : PALETTE) {
			double dr = r - c.r();
			double dg = g - c.g();
			double db = b - c.b();
			double dist = dr * dr + dg * dg + db * db;
			if (dist < bestDist) {
				bestDist = dist;
				best = c;
			}
		}
		return best.name();
	}

	/** Lấy màu chủ đạo từ bytes ảnh (bỏ pixel gần trắng/đen nền nếu có thể). */
	public static Optional<String> fromImageBytes(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return Optional.empty();
		}
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			BufferedImage img = ImageIO.read(in);
			if (img == null) {
				return Optional.empty();
			}
			int w = img.getWidth();
			int h = img.getHeight();
			int step = Math.max(1, Math.min(w, h) / 64);
			long sumR = 0, sumG = 0, sumB = 0, count = 0;
			for (int y = 0; y < h; y += step) {
				for (int x = 0; x < w; x += step) {
					int argb = img.getRGB(x, y);
					int a = (argb >> 24) & 0xff;
					if (a < 128) {
						continue;
					}
					int r = (argb >> 16) & 0xff;
					int g = (argb >> 8) & 0xff;
					int b = argb & 0xff;
					// bỏ nền trắng/đen quá cực; ưu tiên pixel có bão hòa
					int max = Math.max(r, Math.max(g, b));
					int min = Math.min(r, Math.min(g, b));
					if (max > 245 && min > 230) {
						continue;
					}
					if (max < 20) {
						continue;
					}
					int sat = max == 0 ? 0 : (max - min);
					int weight = 1 + sat / 16;
					sumR += (long) r * weight;
					sumG += (long) g * weight;
					sumB += (long) b * weight;
					count += weight;
				}
			}
			if (count == 0) {
				return Optional.of("Đen");
			}
			return Optional.of(nearestName((int) (sumR / count), (int) (sumG / count), (int) (sumB / count)));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Cloudinary colors: List of [hex, percent] hoặc Map.
	 */
	@SuppressWarnings("unchecked")
	public static Optional<String> fromCloudinaryColors(Object colorsObj) {
		if (!(colorsObj instanceof List<?> list) || list.isEmpty()) {
			return Optional.empty();
		}
		String bestHex = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (Object row : list) {
			String hex = null;
			double pct = 0;
			if (row instanceof List<?> pair && pair.size() >= 2) {
				hex = String.valueOf(pair.get(0));
				pct = toDouble(pair.get(1));
			} else if (row instanceof Object[] pair && pair.length >= 2) {
				hex = String.valueOf(pair[0]);
				pct = toDouble(pair[1]);
			}
			if (hex == null) {
				continue;
			}
			int[] rgb = parseHex(hex);
			if (rgb == null) {
				continue;
			}
			int r = rgb[0], g = rgb[1], b = rgb[2];
			int max = Math.max(r, Math.max(g, b));
			int min = Math.min(r, Math.min(g, b));
			double sat = max == 0 ? 0 : (max - min) / (double) max;
			boolean nearWhite = r > 235 && g > 235 && b > 235;
			boolean nearGray = sat < 0.12 && max > 60 && max < 220;
			double score = pct + sat * 55;
			if (nearWhite) {
				score -= 70;
			}
			if (nearGray) {
				score -= 15;
			}
			if (score > bestScore) {
				bestScore = score;
				bestHex = hex;
			}
		}
		if (bestHex == null) {
			Object first = list.get(0);
			if (first instanceof List<?> pair && !pair.isEmpty()) {
				bestHex = String.valueOf(pair.get(0));
			}
		}
		return bestHex != null ? Optional.of(fromHex(bestHex)) : Optional.empty();
	}

	public static Optional<String> fromImageUrl(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			return Optional.empty();
		}
		try (InputStream in = URI.create(imageUrl).toURL().openStream()) {
			byte[] bytes = in.readAllBytes();
			return fromImageBytes(bytes);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public static String sanitizeFolderSegment(String value) {
		if (value == null || value.isBlank()) {
			return "variant";
		}
		String s = value.trim().replaceAll("[\\\\/]+", " ").replaceAll("\\s+", " ");
		if (s.length() > 80) {
			s = s.substring(0, 80).trim();
		}
		return s.isBlank() ? "variant" : s;
	}

	public static List<String> suggestedColors() {
		List<String> names = new ArrayList<>();
		for (NamedColor c : PALETTE) {
			names.add(c.name());
		}
		return names;
	}

	private static double toDouble(Object o) {
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(o));
		} catch (Exception e) {
			return 0;
		}
	}

	private static int[] parseHex(String hex) {
		if (hex == null) {
			return null;
		}
		String h = hex.trim();
		if (h.startsWith("#")) {
			h = h.substring(1);
		}
		if (h.length() == 3) {
			h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
		}
		if (h.length() != 6) {
			return null;
		}
		try {
			return new int[] {
					Integer.parseInt(h.substring(0, 2), 16),
					Integer.parseInt(h.substring(2, 4), 16),
					Integer.parseInt(h.substring(4, 6), 16)
			};
		} catch (Exception e) {
			return null;
		}
	}
}
