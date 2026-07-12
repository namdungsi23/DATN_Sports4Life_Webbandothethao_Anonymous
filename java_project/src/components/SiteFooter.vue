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
            <img :src="logoFull" alt="Sports4Life" class="site-footer__logo-img" width="200" height="46" />
          </RouterLink>
          <p class="site-footer__desc">
            Cửa hàng giày thể thao chính hãng hàng đầu Việt Nam. Cam kết chất lượng, giá tốt và dịch vụ tận tâm.
          </p>
          <div class="site-footer__contact-item">
            <span>📍</span>
            <span>123 Nguyễn Văn Linh, Q.7, TP.HCM</span>
          </div>
          <div class="site-footer__contact-item">
            <span>📞</span>
            <span>Hotline: <strong>0336 694 988</strong> (8h–22h)</span>
          </div>
          <div class="site-footer__contact-item">
            <span>✉</span>
            <span>support@sports4life.vn</span>
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
            <a href="#" aria-label="Facebook">f</a>
            <a href="#" aria-label="Instagram">ig</a>
            <a href="#" aria-label="TikTok">tt</a>
            <a href="#" aria-label="YouTube">yt</a>
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
import logoFull from "../assets/logo-sports4life.svg";

const toast = useToast();
const email = ref("");
const subscribeMsg = ref("");

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
