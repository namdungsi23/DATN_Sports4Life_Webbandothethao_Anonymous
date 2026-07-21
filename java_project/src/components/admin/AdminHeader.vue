<template>
    <header class="play-header">
      <div class="play-logo"><span>▶</span> Admin Play Sport</div>
  
      <nav class="play-menu">
        <RouterLink to="/admin/dashboard">Dashboard</RouterLink>
        <RouterLink to="/admin/product">Sản phẩm</RouterLink>
        <RouterLink to="/admin/category">Danh mục</RouterLink>
        <RouterLink to="/admin/user">Tài khoản người dùng</RouterLink>
        <RouterLink to="/admin/order">Đơn hàng</RouterLink>
        <button type="button" class="play-logout" @click="handleLogout">Đăng xuất</button>
        <RouterLink to="/product">Quay về trang chủ</RouterLink>
      </nav>
  
      <div class="play-right">
        <div class="avatar">:)</div>
      </div>
    </header>
  </template>
  
  <script setup>
  import { RouterLink, useRouter } from "vue-router";
  import { useAppStore } from "../../stores/appStore.js";

  const router = useRouter();
  const store = useAppStore();

  function handleLogout() {
    store.logout();
    router.push("/login");
  }
  </script>
  
  <style scoped>
  .play-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 24px; border-bottom: 1px solid #e5e5e5; }
  .play-logo { font-size: 22px; font-weight: 700; display: flex; align-items: center; gap: 8px; }
  .play-logo span { color: #e60023; font-size: 24px; }
  .play-menu { display: flex; gap: 20px; }
  .play-menu a { text-decoration: none; color: #333; }
  .play-logout {
    border: none;
    background: none;
    padding: 0;
    font: inherit;
    color: #c00;
    cursor: pointer;
    text-decoration: underline;
  }
  .play-logout:hover { color: #900; }
  .play-right { display: flex; align-items: center; gap: 14px; }
  .avatar { width: 34px; height: 34px; background: #ddd; border-radius: 50%; display: grid; place-items: center; }
  </style>
  