<template>
  <footer class="site-footer">
    <div class="site-footer__newsletter">
      <div class="site-container site-footer__newsletter-inner">
        <div class="site-footer__newsletter-text">
          <h3>Đăng ký nhận tin khuyến mãi</h3>
          <p>Nhận ngay voucher 50K cho đơn hàng đầu tiên</p>
        </div>
        <form class="site-footer__newsletter-form" @submit.prevent="onSubscribe">
          <input v-model="email" type="email" placeholder="Nhập email của bạn" required />
          <button type="submit">Đăng ký</button>
        </form>
      </div>
    </div>

    <div class="site-footer__main">
      <div class="site-container site-footer__grid">
        <div class="site-footer__brand">
          <RouterLink to="/" class="site-footer__logo" aria-label="Sports4Life trang chủ">
            <img
              :src="BRAND.logoUrl"
              alt="Sports4Life"
              class="site-footer__logo-img"
              width="200"
              height="46"
            />
          </RouterLink>
          <p class="site-footer__desc">
            Cửa hàng giày thể thao chính hãng hàng đầu Việt Nam. Cam kết chất lượng, giá tốt và dịch vụ tận tâm.
          </p>
          <div class="site-footer__contact-item">
            <span class="site-footer__contact-ico" aria-hidden="true">📍</span>
            <span>{{ BRAND.addressShort }}</span>
          </div>
          <div class="site-footer__contact-item">
            <span class="site-footer__contact-ico" aria-hidden="true">📞</span>
            <span>Hotline: <strong>{{ BRAND.phone }}</strong> ({{ BRAND.hours }})</span>
          </div>
          <div class="site-footer__contact-item">
            <span class="site-footer__contact-ico" aria-hidden="true">✉</span>
            <a :href="`mailto:${BRAND.email}`" class="site-footer__mail">{{ BRAND.email }}</a>
          </div>
        </div>

        <div class="site-footer__col">
          <h4>Về Sports4Life</h4>
          <RouterLink to="/more">Giới thiệu</RouterLink>
          <RouterLink to="/more">Tuyển dụng</RouterLink>
          <RouterLink to="/more">Hệ thống cửa hàng</RouterLink>
          <RouterLink to="/contact">Liên hệ</RouterLink>
        </div>

        <div class="site-footer__col">
          <h4>Hỗ trợ khách hàng</h4>
          <a href="#">Hướng dẫn mua hàng</a>
          <a href="#">Chính sách đổi trả</a>
          <a href="#">Chính sách bảo hành</a>
          <a href="#">Phương thức thanh toán</a>
          <a href="#">Tra cứu đơn hàng</a>
        </div>

        <div class="site-footer__col">
          <h4>Danh mục sản phẩm</h4>
          <RouterLink to="/product">Giày chạy bộ</RouterLink>
          <RouterLink to="/product">Giày bóng đá</RouterLink>
          <RouterLink to="/product">Giày lifestyle</RouterLink>
          <RouterLink to="/product">Giày trẻ em</RouterLink>
          <RouterLink to="/product">Phụ kiện thể thao</RouterLink>
        </div>

        <div class="site-footer__col">
          <h4>Kết nối với chúng tôi</h4>
          <div class="site-footer__social">
            <a
              v-for="s in socials"
              :key="s.label"
              :href="s.href"
              :aria-label="s.label"
              target="_blank"
              rel="noopener noreferrer"
              class="site-footer__social-btn"
              v-html="s.svg"
            />
          </div>
          <p class="site-footer__app-label">Tải ứng dụng</p>
          <div class="site-footer__apps">
            <a href="#" class="site-footer__app-btn">App Store</a>
            <a href="#" class="site-footer__app-btn">Google Play</a>
          </div>
        </div>
      </div>
    </div>

    <div class="site-footer__payments">
      <div class="site-container site-footer__payments-inner">
        <div class="site-footer__payment-group">
          <span>Thanh toán:</span>
          <div class="site-footer__badges">
            <span>VISA</span>
            <span>Mastercard</span>
            <span>Momo</span>
            <span>VNPAY</span>
            <span>COD</span>
          </div>
        </div>
        <div class="site-footer__payment-group">
          <span>Vận chuyển:</span>
          <div class="site-footer__badges">
            <span>GHTK</span>
            <span>GHN</span>
            <span>Viettel Post</span>
            <span>J&amp;T</span>
          </div>
        </div>
      </div>
    </div>

    <div class="site-footer__bottom">
      <div class="site-container site-footer__bottom-inner">
        <p>© 2026 Sports4Life. Bản quyền thuộc Công ty TNHH Thương mại Thể thao Sports4Life.</p>
        <div class="site-footer__legal">
          <a href="#">Điều khoản sử dụng</a>
          <a href="#">Chính sách bảo mật</a>
          <span class="site-footer__bct">Đã thông báo Bộ Công Thương</span>
        </div>
      </div>
    </div>

    <p v-if="subscribeMsg" class="site-footer__toast">{{ subscribeMsg }}</p>
  </footer>
</template>

<script setup>
import { ref } from "vue";
import { RouterLink } from "vue-router";
import { useToast } from "../stores/appStore";
import { firstError, runValidation } from "../utils/validators";
import { BRAND } from "../utils/brand";

const toast = useToast();
const email = ref("");
const subscribeMsg = ref("");

const socials = [
  {
    label: "Facebook",
    href: "https://facebook.com",
    svg: `<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="currentColor"><path d="M14 9h3V6h-3c-1.7 0-3 1.3-3 3v2H8v3h3v7h3v-7h2.5l.5-3H14V9z"/></svg>`,
  },
  {
    label: "Instagram",
    href: "https://instagram.com",
    svg: `<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="currentColor"><path d="M7 2h10a5 5 0 0 1 5 5v10a5 5 0 0 1-5 5H7a5 5 0 0 1-5-5V7a5 5 0 0 1 5-5zm5 5a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm6.5-.9a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 0 0 0-2.2zM12 9a3 3 0 1 1 0 6 3 3 0 0 1 0-6z"/></svg>`,
  },
  {
    label: "TikTok",
    href: "https://tiktok.com",
    svg: `<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="currentColor"><path d="M14.5 3c.4 2.3 1.9 4.1 4 4.7v2.4c-1.4-.1-2.7-.6-3.8-1.4v6.2a5.5 5.5 0 1 1-5.5-5.5c.3 0 .6 0 .9.1v2.6a2.9 2.9 0 1 0 2 2.8V3h2.4z"/></svg>`,
  },
  {
    label: "YouTube",
    href: "https://youtube.com",
    svg: `<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="currentColor"><path d="M23 12.2s0-3.4-.4-5c-.2-1-.9-1.7-1.8-2-1.6-.4-8-.4-8-.4s-6.4 0-8 .4c-1 .2-1.6 1-1.8 2C3 8.8 3 12.2 3 12.2s0 3.4.4 5c.2 1 .9 1.7 1.8 2 1.6.4 8 .4 8 .4s6.4 0 8-.4c1-.2 1.6-1 1.8-2 .4-1.6.4-5 .4-5zM10 15.5v-6.6l5.5 3.3-5.5 3.3z"/></svg>`,
  },
];

const onSubscribe = () => {
  const result = runValidation(
    { email: email.value },
    { email: ["required", "email"] }
  );
  if (!result.ok) {
    toast.error(firstError(result.errors));
    return;
  }
  subscribeMsg.value = "Cảm ơn bạn đã đăng ký nhận tin!";
  toast.success(subscribeMsg.value);
  email.value = "";
  setTimeout(() => {
    subscribeMsg.value = "";
  }, 3000);
};
</script>
