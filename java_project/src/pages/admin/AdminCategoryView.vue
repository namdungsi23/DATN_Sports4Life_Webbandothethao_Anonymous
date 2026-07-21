<template>
    <AdminLayout>
      <AdminReadOnlyNotice />
      <div class="container-fluid px-4 mt-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
          <h4 class="fw-bold mb-0">Quản lý danh mục</h4>
        </div>
  
        <div v-if="flashOk" class="alert alert-success">{{ flashOk }}</div>
        <div v-if="flashErr" class="alert alert-danger">{{ flashErr }}</div>
        <div v-if="err" class="alert alert-danger">{{ err }}</div>
  
        <form class="row g-2 mb-3" @submit.prevent="search">
          <div class="col-md-6">
            <input v-model="keyword" type="text" class="form-control" placeholder="Tìm theo tên danh mục" />
          </div>
          <div class="col-md-3">
            <select v-model="size" class="form-select" @change="search">
              <option value="all">Tất cả danh mục</option>
              <option value="3">3 dòng</option>
              <option value="10">10 dòng</option>
              <option value="12">12 dòng</option>
            </select>
          </div>
          <div class="col-md-3">
            <button type="submit" class="btn btn-primary w-100">Tìm kiếm</button>
          </div>
        </form>
  
        <div class="row g-4">
          <div class="col-lg-4">
            <div class="card shadow border-0">
              <div
                class="card-header"
                :class="form.id ? 'bg-warning text-dark' : 'bg-primary text-white'"
              >
                <strong>{{ form.id ? "Cập nhật danh mục" : "Thêm danh mục" }}</strong>
              </div>
              <div class="card-body">
                <fieldset :disabled="!canWrite">
                <form @submit.prevent="saveCategory">
                  <div v-if="form.id" class="mb-2">
                    <label class="form-label fw-semibold">Mã danh mục</label>
                    <input v-model="form.id" type="text" class="form-control" readonly />
                  </div>
                  <div class="mb-3">
                    <label class="form-label fw-semibold">Tên danh mục</label>
                    <input
                      v-model="form.name"
                      type="text"
                      class="form-control"
                      placeholder="Nhập tên danh mục"
                      required
                    />
                  </div>
                  <button type="submit" class="btn btn-success w-100 fw-semibold">Lưu danh mục</button>
                  <button v-if="form.id" type="button" class="btn btn-outline-secondary w-100 mt-2" @click="resetForm">
                    Làm mới
                  </button>
                </form>
                </fieldset>
              </div>
            </div>
          </div>
  
          <div class="col-lg-8">
            <div class="card shadow border-0">
              <div class="card-header bg-dark text-white fw-semibold"> Danh sách danh mục</div>
              <div class="card-body p-0">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light">
                    <tr>
                      <th style="width: 80px">ID</th>
                      <th>Tên danh mục</th>
                      <th style="width: 160px">Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="c in categories" :key="c.id">
                      <td>{{ c.id }}</td>
                      <td class="fw-semibold">{{ c.name }}</td>
                      <td>
                        <template v-if="canWrite">
                        <button type="button" class="btn btn-sm btn-outline-warning me-1" @click="editRow(c)">
                          Sửa
                        </button>
                        <button
                          type="button"
                          class="btn btn-sm btn-outline-danger"
                          @click="remove(c.id)"
                        >
                          Xóa
                        </button>
                        </template>
                        <span v-else class="text-muted small">Chỉ xem</span>
                      </td>
                    </tr>
                    <tr v-if="!categories.length">
                      <td colspan="3" class="text-center text-muted py-4">Không có danh mục nào</td>
                    </tr>
                  </tbody>
                </table>
                <nav v-if="totalPages > 1">
                  <ul class="pagination justify-content-center mt-3">
                    <li class="page-item" :class="{ disabled: currentPage === 0 }">
                      <a class="page-link" href="#" @click.prevent="goPage(currentPage - 1)">‹</a>
                    </li>
                    <li
                      v-for="i in totalPages"
                      :key="i"
                      class="page-item"
                      :class="{ active: i - 1 === currentPage }"
                    >
                      <a class="page-link" href="#" @click.prevent="goPage(i - 1)">{{ i }}</a>
                    </li>
                    <li class="page-item" :class="{ disabled: currentPage + 1 >= totalPages }">
                      <a class="page-link" href="#" @click.prevent="goPage(currentPage + 1)">›</a>
                    </li>
                  </ul>
                </nav>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AdminLayout>
  </template>
  <script setup>
  import { computed, onMounted, reactive, ref } from "vue";
  import AdminLayout from "../../layouts/AdminLayout.vue";
  import AdminReadOnlyNotice from "../../components/admin/AdminReadOnlyNotice.vue";
  import { apiFetch } from "../../services/http.js";
  import { useAppStore } from "../../stores/appStore";
  import { userCanWriteCatalog } from "../../utils/adminAccess";

  const store = useAppStore();
  const canWrite = computed(() => userCanWriteCatalog(store.state.user));

  const err = ref("");
  const flashOk = ref("");
  const flashErr = ref("");
  const keyword = ref("");
  const size = ref("all");
  const currentPage = ref(0);
  const totalPages = ref(1);
  const categories = ref([]);
  const form = reactive({ id: "", name: "" });
  
  async function fetchList() {
    err.value = "";
    const params = new URLSearchParams({ page: String(currentPage.value), keyword: keyword.value, size: size.value });
    const data = await apiFetch(`/api/admin/categories?${params}`);
    categories.value = data.categories || [];
    totalPages.value = data.totalPages || 1;
    currentPage.value = data.currentPage ?? 0;
  }
  
  function search() {
    currentPage.value = 0;
    fetchList().catch((e) => {
      err.value = e.message || "Lỗi tải";
    });
  }
  
  function goPage(p) {
    if (p < 0 || p >= totalPages.value) return;
    currentPage.value = p;
    fetchList().catch((e) => {
      err.value = e.message || "Lỗi tải";
    });
  }
  
  function editRow(c) {
    form.id = c.id;
    form.name = c.name;
  }
  
  function resetForm() {
    form.id = "";
    form.name = "";
  }
  
  async function saveCategory() {
    flashOk.value = "";
    flashErr.value = "";
    err.value = "";
    const name = String(form.name || "").trim();
    if (!name) {
      flashErr.value = "Tên danh mục không được để trống.";
      return;
    }
    if (name.length > 100) {
      flashErr.value = "Tên danh mục tối đa 100 ký tự.";
      return;
    }
    try {
      if (form.id) {
        await apiFetch("/api/admin/categories", {
          method: "PUT",
          body: JSON.stringify({ id: form.id, name }),
        });
        flashOk.value = "Cập nhật thành công";
      } else {
        const res = await apiFetch("/api/admin/categories", {
          method: "POST",
          body: JSON.stringify({ name }),
        });
        flashOk.value = res.message || "Lưu thành công";
      }
      resetForm();
      await fetchList();
    } catch (e) {
      flashErr.value = e.message || "Lưu thất bại";
    }
  }
  
  async function remove(id) {
    if (!confirm("Xóa danh mục này?")) return;
    flashOk.value = "";
    flashErr.value = "";
    try {
      const res = await apiFetch(`/api/admin/categories/${encodeURIComponent(id)}`, { method: "DELETE" });
      flashOk.value = res.message || "Đã xóa";
      await fetchList();
    } catch (e) {
      flashErr.value = e.message || "Xóa thất bại";
    }
  }
  
  onMounted(() => {
    fetchList().catch((e) => {
      err.value = e.message || "Lỗi tải";
    });
  });
  </script>
  