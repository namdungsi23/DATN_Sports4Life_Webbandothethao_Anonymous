<template>
  <div>
    <SiteHeader
      :user="store.state.user"
      :count="store.cartCount.value"
      :favorite-count="store.favoriteCount.value"
      @logout="store.logout"
    />
    <slot name="full" />
    <main class="site-main">
      <div class="site-container site-main__inner">
        <slot />
      </div>
    </main>
    <SiteFooter />
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import SiteHeader from "../components/user/SiteHeader.vue";
import SiteFooter from "../components/user/SiteFooter.vue";
import { fetchProfileApi, ensureAuthSession } from "../services/api";
import { useAppStore } from "../stores/appStore";

const store = useAppStore();

onMounted(async () => {
  if (!store.state.user?.username) return;

  const hasSession = await ensureAuthSession();
  if (!hasSession) return;

  store.loadFavorites();

  try {
    const data = await fetchProfileApi();
    const profile = data?.profile || data;
    if (profile && typeof profile === "object") {
      store.updateUserProfile({
        fullname: profile.fullname || profile.fullName,
        photo: profile.photo || profile.avatar,
        email: profile.email,
        rankId: profile.rankId,
        rankName: profile.rankName,
        totalPoint: profile.totalPoint,
        rankDiscountPercent: profile.rankDiscountPercent,
        rankMinPoint: profile.rankMinPoint,
      });
    }
  } catch {
    /* profile optional */
  }
});
</script>