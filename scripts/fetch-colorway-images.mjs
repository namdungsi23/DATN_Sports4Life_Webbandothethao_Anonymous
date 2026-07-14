/**
 * Tải 4 ảnh theo TỪNG MÀU của sản phẩm (đúng màu hình),
 * upload Cloudinary product/sp{id}/color_{n}/sp{id}_c{n}_{1..4},
 * gán vào mọi biến thể cùng màu.
 *
 * node scripts/fetch-colorway-images.mjs
 * node scripts/fetch-colorway-images.mjs --only=61,46
 * node scripts/fetch-colorway-images.mjs --from=5 --skip=46,61,81,95
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { execSync } from "child_process";
import { v2 as cloudinary } from "cloudinary";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");

const onlyArg = process.argv.find((a) => a.startsWith("--only="));
const onlyIds = onlyArg
  ? onlyArg
      .slice(7)
      .split(",")
      .map((x) => Number(x.trim()))
      .filter(Boolean)
  : null;
const fromArg = process.argv.find((a) => a.startsWith("--from="));
const fromId = fromArg ? Number(fromArg.slice(7)) : null;
const skipArg = process.argv.find((a) => a.startsWith("--skip="));
const skipIds = new Set(
  skipArg
    ? skipArg
        .slice(7)
        .split(",")
        .map((x) => Number(x.trim()))
        .filter(Boolean)
    : []
);

const COLOR_EN = {
  Đen: "black",
  Den: "black",
  Trắng: "white",
  Trang: "white",
  "Xanh Navy": "navy blue",
  Navy: "navy",
  Đỏ: "red",
  Do: "red",
  Xám: "grey gray",
  Xam: "grey",
  "Xanh Lá": "green",
  "Xanh La": "green",
  "Mặc định": "",
};

function loadCloudinary() {
  const text = fs.readFileSync(
    path.join(root, "src/main/resources/application.properties"),
    "utf8"
  );
  const get = (k) => (text.match(new RegExp(`^${k}=(.+)$`, "m")) || [])[1]?.trim();
  cloudinary.config({
    cloud_name: get("cloudinary.cloud-name"),
    api_key: get("cloudinary.api-key"),
    api_secret: get("cloudinary.api-secret"),
    secure: true,
  });
}

function sqlcmd(q) {
  const qf = path.join(__dirname, `_q_${Date.now()}.sql`);
  const of = path.join(__dirname, `_o_${Date.now()}.txt`);
  fs.writeFileSync(qf, q, "utf8");
  try {
    execSync(`sqlcmd -S localhost -d Sports4Life -E -f 65001 -i "${qf}" -o "${of}" -s"|" -W -w 500`, {
      stdio: "pipe",
    });
    return fs.readFileSync(of, "utf8");
  } finally {
    try {
      fs.unlinkSync(qf);
    } catch {}
    try {
      fs.unlinkSync(of);
    } catch {}
  }
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function sqlEsc(s) {
  return String(s).replace(/'/g, "''");
}

async function getVqd(query) {
  const res = await fetch(
    `https://duckduckgo.com/?q=${encodeURIComponent(query)}&iax=images&ia=images`,
    {
      headers: {
        "User-Agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0 Safari/537.36",
      },
    }
  );
  const html = await res.text();
  const m = html.match(/vqd=["']([^"']+)["']/);
  if (!m) throw new Error("no vqd");
  return m[1];
}

async function searchImages(query, max = 10) {
  const vqd = await getVqd(query);
  const api = `https://duckduckgo.com/i.js?l=us-en&o=json&q=${encodeURIComponent(query)}&vqd=${encodeURIComponent(vqd)}&f=,,,,,&p=1`;
  const res = await fetch(api, {
    headers: {
      "User-Agent": "Mozilla/5.0",
      Referer: "https://duckduckgo.com/",
      Accept: "application/json",
    },
  });
  if (!res.ok) throw new Error(`ddg ${res.status}`);
  const data = await res.json();
  const urls = [];
  for (const item of data.results || []) {
    const u = item.image;
    if (!u || !/^https?:\/\//i.test(u) || /\.svg(\?|$)/i.test(u)) continue;
    if (urls.includes(u)) continue;
    urls.push(u);
    if (urls.length >= max) break;
  }
  return urls;
}

async function uploadColorAngle(sourceUrl, productId, colorNo, sortOrder) {
  const publicId = `product/sp${productId}/color_${colorNo}/sp${productId}_c${colorNo}_${sortOrder}`;
  const result = await cloudinary.uploader.upload(sourceUrl, {
    public_id: publicId,
    overwrite: true,
    invalidate: true,
    resource_type: "image",
    format: "jpg",
    transformation: [{ width: 1200, height: 1200, crop: "limit", quality: "auto" }],
  });
  return result.secure_url || result.url;
}

function loadCatalog() {
  const raw = sqlcmd(`SET NOCOUNT ON;
SELECT p.Id, p.Name, LTRIM(RTRIM(v.Color)) AS Color, v.Id AS VariantId, ISNULL(v.DisplayOrder,9999) AS Ord
FROM Products p
JOIN ProductVariants v ON v.ProductId=p.Id
ORDER BY p.Id, Ord, v.Id;`);
  const lines = raw.split(/\r?\n/).filter((l) => l && !l.startsWith("-") && !l.startsWith("Id|"));
  const map = new Map();
  for (const line of lines) {
    const parts = line.split("|");
    if (parts.length < 5) continue;
    const id = Number(parts[0]);
    const name = parts[1]?.trim();
    const color = parts[2]?.trim();
    const variantId = Number(parts[3]);
    if (!id || !name || !color || !variantId) continue;
    if (onlyIds && !onlyIds.includes(id)) continue;
    if (fromId != null && !Number.isNaN(fromId) && id < fromId) continue;
    if (skipIds.has(id)) continue;
    if (!map.has(id)) map.set(id, { id, name, colors: new Map() });
    const p = map.get(id);
    if (!p.colors.has(color)) p.colors.set(color, []);
    p.colors.get(color).push(variantId);
  }
  return [...map.values()];
}

async function main() {
  loadCloudinary();
  const products = loadCatalog();
  console.log(`Products: ${products.length}`);

  let ok = 0;
  let fail = 0;
  const allSql = [];

  for (const p of products) {
    const colorEntries = [...p.colors.entries()];
    console.log(`\nSP${p.id} ${p.name} — ${colorEntries.length} colors`);
    const productSql = [];
    let colorNo = 0;

    for (const [color, variantIds] of colorEntries) {
      colorNo++;
      const en = COLOR_EN[color] || color;
      const queries = [
        `${p.name} ${en} product photo`,
        `${p.name} ${color} official`,
        `${p.name} ${en}`,
      ];
      let found = [];
      for (const q of queries) {
        try {
          found = await searchImages(q, 12);
          if (found.length >= 4) break;
        } catch (e) {
          console.warn(`  search fail [${color}]: ${e.message}`);
        }
        await sleep(350);
      }
      if (!found.length) {
        console.warn(`  no images for ${color}`);
        fail++;
        continue;
      }
      while (found.length < 4) found.push(found[found.length - 1]);
      found = found.slice(0, 4);

      const urls = [];
      for (let i = 0; i < 4; i++) {
        try {
          const url = await uploadColorAngle(found[i], p.id, colorNo, i + 1);
          urls.push(url);
          await sleep(100);
        } catch (e) {
          console.warn(`  upload fail ${color} #${i + 1}: ${e.message}`);
        }
      }
      while (urls.length > 0 && urls.length < 4) urls.push(urls[urls.length - 1]);
      if (!urls.length) {
        fail++;
        continue;
      }

      console.log(`  ${color} → ${urls.length} angles → variants ${variantIds.join(",")}`);
      productSql.push(`DELETE FROM ProductImages WHERE VariantId IN (${variantIds.join(",")});`);
      for (const vid of variantIds) {
        for (let i = 0; i < 4; i++) {
          productSql.push(
            `INSERT INTO ProductImages (ImageUrl, IsDefault, SortOrder, CreatedAt, VariantId) VALUES (N'${sqlEsc(urls[i])}', ${i === 0 ? 1 : 0}, ${i + 1}, SYSUTCDATETIME(), ${vid});`
          );
        }
      }
      ok++;
    }

    if (productSql.length) {
      const batch = [
        "SET XACT_ABORT ON; SET NOCOUNT ON; BEGIN TRANSACTION;",
        ...productSql,
        "COMMIT;",
      ].join("\n");
      try {
        sqlcmd(batch);
        console.log(`  ✓ SQL applied SP${p.id}`);
      } catch (e) {
        console.warn(`  SQL apply fail SP${p.id}: ${e.message}`);
        allSql.push(`-- SP${p.id}\n${batch}`);
      }
    }
  }

  if (allSql.length) {
    const out = path.join(__dirname, "fetch-colorway-images-generated.sql");
    fs.writeFileSync(out, allSql.join("\n\n"), "utf8");
    console.log(`Pending SQL: ${out}`);
  }
  console.log({ ok, fail });
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
