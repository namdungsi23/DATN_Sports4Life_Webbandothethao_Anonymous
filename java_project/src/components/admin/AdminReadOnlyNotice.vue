<template>
  <div v-if="!canWrite" class="alert alert-info py-2 mb-3">
    Chế độ chỉ xem — nhân viên không có quyền thêm, sửa hoặc xóa trên trang này.
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useAppStore } from "../../stores/appStore";
import { userCanWriteCatalog } from "../../utils/adminAccess";

const store = useAppStore();
const canWrite = computed(() => userCanWriteCatalog(store.state.user));
</script>
