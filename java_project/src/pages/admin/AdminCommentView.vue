<template>
  <AdminLayout>
    <div class="container-fluid px-4 mt-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 class="fw-bold mb-1">Quản lý bình luận</h4>
          <p class="text-muted mb-0">Ẩn / hiện hoặc xóa đánh giá của khách hàng</p>
        </div>
      </div>

      <div v-if="flashOk" class="alert alert-success">{{ flashOk }}</div>
      <div v-if="flashErr" class="alert alert-danger">{{ flashErr }}</div>

      <form class="row g-2 mb-3" @submit.prevent="load">
        <div class="col-md-5">
          <input
            v-model="keyword"
            type="text"
            class="form-control"
            placeholder="Tìm theo nội dung, khách, sản phẩm..."
          />
        </div>
        <div class="col-md-3">
          <select v-model="visibleFilter" class="form-select" @change="load">
            <option value="all">Tất cả trạng thái</option>
            <option value="true">Đang hiện</option>
            <option value="false">Đã ẩn</option>
          </select>
        </div>
        <div class="col-md-2">
          <button type="submit" class="btn btn-primary w-100" :disabled="loading">
            {{ loading ? "Đang tải..." : "Tìm kiếm" }}
          </button>
        </div>
        <div class="col-md-2">
          <button type="button" class="btn btn-outline-secondary w-100" @click="resetFilters">Làm mới</button>
        </div>
      </form>

      <div class="card shadow border-0">
        <div class="card-header bg-dark text-white fw-semibold">
          Danh sách bình luận ({{ comments.length }})
        </div>
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th style="width: 70px">ID</th>
                  <th>Sản phẩm</th>
                  <th>Khách</th>
                  <th style="width: 90px">Sao</th>
                  <th>Nội dung</th>
                  <th style="width: 100px">Trạng thái</th>
                  <th style="width: 140px">Thời gian</th>
                  <th style="width: 200px">Hành động</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!loading && !comments.length">
                  <td colspan="8" class="text-center text-muted py-4">Chưa có bình luận</td>
                </tr>
                <tr v-for="c in comments" :key="c.id">
                  <td>{{ c.id }}</td>
                  <td>
                    <div class="fw-semibold">{{ c.productName || ("#" + c.productId) }}</div>
                    <small class="text-muted">SP #{{ c.productId }}</small>
                  </td>
                  <td>
                    <div>{{ c.fullName || c.username || "—" }}</div>
                    <small v-if="c.rankName" class="badge text-bg-light border">{{ c.rankName }}</small>
                  </td>
                  <td>
                    <span class="text-warning">{{ "★".repeat(c.rating || 0) }}</span>
                    <span class="text-muted">{{ "☆".repeat(5 - (c.rating || 0)) }}</span>
                  </td>
                  <td style="max-width: 280px">
                    <div class="text-truncate" :title="c.content">{{ c.content }}</div>
                  </td>
                  <td>
                    <span
                      class="badge"
                      :class="c.status ? 'text-bg-success' : 'text-bg-secondary'"
                    >
                      {{ c.status ? "Hiện" : "Ẩn" }}
                    </span>
                  </td>
                  <td>
                    <small>{{ formatTime(c.createdAt) }}</small>
                  </td>
                  <td>
                    <div class="d-flex flex-wrap gap-1">
                      <button
                        type="button"
                        class="btn btn-sm"
                        :class="c.status ? 'btn-outline-warning' : 'btn-outline-success'"
                        :disabled="busyId === c.id"
                        @click="toggleVisible(c)"
                      >
                        {{ c.status ? "Ẩn" : "Hiện" }}
                      </button>
                      <button
                        type="button"
                        class="btn btn-sm btn-outline-danger"
                        :disabled="busyId === c.id"
                        @click="remove(c)"
                      >
                        Xóa
                      </button>
                      <RouterLink
                        v-if="c.productId"
                        :to="`/product/${c.productId}`"
                        class="btn btn-sm btn-outline-primary"
                        target="_blank"
                      >
                        Xem SP
                      </RouterLink>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import AdminLayout from "../../layouts/AdminLayout.vue";
import { apiFetch } from "../../services/http.js";
import { useToast } from "../../stores/appStore";

const toast = useToast();
const comments = ref([]);
const keyword = ref("");
const visibleFilter = ref("all");
const loading = ref(false);
const busyId = ref(null);
const flashOk = ref("");
const flashErr = ref("");

const formatTime = (value) => {
  if (!value) return "";
  return new Date(value).toLocaleString("vi-VN");
};

const resetFilters = () => {
  keyword.value = "";
  visibleFilter.value = "all";
  load();
};

const load = async () => {
  loading.value = true;
  flashErr.value = "";
  try {
    const params = new URLSearchParams();
    if (keyword.value.trim()) params.set("keyword", keyword.value.trim());
    if (visibleFilter.value === "true") params.set("visible", "true");
    if (visibleFilter.value === "false") params.set("visible", "false");
    const qs = params.toString();
    const data = await apiFetch(`/api/admin/comments${qs ? `?${qs}` : ""}`);
    comments.value = data.comments || [];
  } catch (e) {
    flashErr.value = e?.message || "Không tải được bình luận.";
    comments.value = [];
  } finally {
    loading.value = false;
  }
};

const toggleVisible = async (c) => {
  busyId.value = c.id;
  flashOk.value = "";
  flashErr.value = "";
  try {
    const next = !c.status;
    const data = await apiFetch(`/api/admin/comments/${c.id}/visible?value=${next}`, {
      method: "PATCH",
    });
    flashOk.value = data.message || "Đã cập nhật.";
    toast.success(flashOk.value);
    await load();
  } catch (e) {
    flashErr.value = e?.message || "Cập nhật thất bại.";
    toast.error(flashErr.value);
  } finally {
    busyId.value = null;
  }
};

const remove = async (c) => {
  if (!confirm(`Xóa bình luận #${c.id}?`)) return;
  busyId.value = c.id;
  flashOk.value = "";
  flashErr.value = "";
  try {
    const data = await apiFetch(`/api/admin/comments/${c.id}`, { method: "DELETE" });
    flashOk.value = data.message || "Đã xóa.";
    toast.success(flashOk.value);
    await load();
  } catch (e) {
    flashErr.value = e?.message || "Xóa thất bại.";
    toast.error(flashErr.value);
  } finally {
    busyId.value = null;
  }
};

onMounted(load);
</script>
