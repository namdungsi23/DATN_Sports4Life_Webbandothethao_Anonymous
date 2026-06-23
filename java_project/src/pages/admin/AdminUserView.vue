<template>
  <AdminLayout>
    <div class="container-fluid">
      <h3 class="mb-4">👤 Quản lý người dùng</h3>
      <div v-if="err" class="alert alert-danger">{{ err }}</div>
      <div v-if="okMsg" class="alert alert-success">{{ okMsg }}</div>

      <form class="d-flex mb-4 p-3 bg-light rounded shadow-sm" @submit.prevent="search">
        <input
          v-model="keyword"
          type="text"
          class="form-control form-control-lg me-2 border-primary"
          placeholder="🔍 Tìm username hoặc họ tên"
        />
        <button type="submit" class="btn btn-primary btn-lg px-4">Tìm kiếm</button>
      </form>

      <div class="row">
        <div class="col-md-4">
          <div class="card shadow-sm">
            <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
              <strong>Tạo | Cập nhật người dùng</strong>
              <button type="button" class="btn btn-sm btn-light" @click="clearForm">+ Tạo mới</button>
            </div>
            <div class="card-body">
              <form @submit.prevent="saveUser">
                <div class="mb-3">
                  <label class="form-label"><strong>Tên đăng nhập</strong></label>
                  <input
                    v-model="form.username"
                    type="text"
                    class="form-control"
                    :readonly="isEditing"
                    required
                  />
                </div>
                <div v-if="!isEditing" class="mb-3">
                  <label class="form-label"><strong>Mật khẩu</strong></label>
                  <input
                    v-model="form.password"
                    type="password"
                    class="form-control"
                    placeholder="Mặc định: 123 nếu để trống"
                    autocomplete="new-password"
                  />
                </div>
                <div class="mb-3">
                  <label class="form-label"><strong>Họ tên</strong></label>
                  <input v-model="form.fullname" type="text" class="form-control" />
                </div>
                <div class="mb-3">
                  <label class="form-label"><strong>Email</strong></label>
                  <input v-model="form.email" type="email" class="form-control" />
                </div>
                <div class="mb-3">
                  <label class="form-label"><strong>Ảnh đại diện (URL)</strong></label>
                  <input v-model="form.photo" type="text" class="form-control" />
                </div>
                <div class="mb-3">
                  <label class="form-label d-block"><strong>Vai trò</strong></label>
                  <div v-for="role in roleOptions" :key="role" class="form-check">
                    <input
                      :id="'role-' + role"
                      class="form-check-input"
                      type="checkbox"
                      :checked="form.roles.includes(role)"
                      @change="toggleRole(role, $event.target.checked)"
                    />
                    <label class="form-check-label" :for="'role-' + role">{{ role }}</label>
                  </div>
                </div>
                <div class="form-check mb-3">
                  <input id="act" v-model="form.activated" class="form-check-input" type="checkbox" />
                  <label class="form-check-label" for="act"><strong>Kích hoạt tài khoản</strong></label>
                </div>
                <button type="submit" class="btn btn-success w-100">💾 Lưu</button>
              </form>
            </div>
          </div>
        </div>

        <div class="col-md-8">
          <div class="card shadow-sm">
            <div class="card-header bg-dark text-white">Danh sách người dùng</div>
            <div class="card-body table-responsive">
              <table class="table table-bordered table-hover align-middle">
                <thead class="table-light">
                  <tr>
                    <th>Username</th>
                    <th>Họ tên</th>
                    <th>Email</th>
                    <th>Vai trò</th>
                    <th>Trạng thái</th>
                    <th class="text-center">Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="u in users" :key="u.username">
                    <td>{{ u.username }}</td>
                    <td>{{ u.fullname }}</td>
                    <td>{{ u.email }}</td>
                    <td>
                      <template v-if="u.roleNames?.length">
                        <span
                          v-for="r in u.roleNames"
                          :key="r"
                          class="badge me-1"
                          :class="r === 'ROLE_ADMIN' ? 'bg-success' : r === 'ROLE_STAFF' ? 'bg-info' : 'bg-secondary'"
                          >{{ r }}</span
                        >
                      </template>
                      <span v-else class="text-muted small">—</span>
                    </td>
                    <td>
                      <span v-if="u.activated" class="badge bg-primary">Hoạt động</span>
                      <span v-else class="badge bg-danger">Khóa</span>
                    </td>
                    <td class="text-center">
                      <button type="button" class="btn btn-sm btn-warning" @click="loadEdit(u)">✏️</button>
                    </td>
                  </tr>
                </tbody>
              </table>
              <nav>
                <ul class="pagination justify-content-center">
                  <li class="page-item" :class="{ disabled: pages.first }">
                    <a class="page-link" href="#" @click.prevent="goPage(pages.number - 1)">«</a>
                  </li>
                  <li
                    v-for="i in pages.totalPages"
                    :key="i"
                    class="page-item"
                    :class="{ active: i - 1 === pages.number }"
                  >
                    <a class="page-link" href="#" @click.prevent="goPage(i - 1)">{{ i }}</a>
                  </li>
                  <li class="page-item" :class="{ disabled: pages.last }">
                    <a class="page-link" href="#" @click.prevent="goPage(pages.number + 1)">»</a>
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
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AdminLayout from "../../layouts/AdminLayout.vue";
import { apiFetch } from "../../services/http.js";

const route = useRoute();
const router = useRouter();
const err = ref("");
const okMsg = ref("");
const keyword = ref("");
const users = ref([]);
const authorities = ref([]);
const pages = reactive({
  number: 0,
  totalPages: 1,
  first: true,
  last: true,
});
const form = reactive({
  username: "",
  password: "",
  fullname: "",
  email: "",
  photo: "",
  activated: true,
  roles: [],
});
const editingUsername = ref(null);

const isEditing = computed(() => editingUsername.value != null && editingUsername.value !== "");

/** Roles from Sport4L DB via API */
const roleOptions = computed(() => {
  const raw = authorities.value || [];
  return raw.length ? raw : ["ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"];
});

function toggleRole(role, checked) {
  const set = new Set(form.roles);
  if (checked) {
    set.add(role);
  } else {
    set.delete(role);
  }
  form.roles = [...set];
}

function clearForm() {
  editingUsername.value = null;
  form.username = "";
  form.password = "";
  form.fullname = "";
  form.email = "";
  form.photo = "";
  form.activated = true;
  form.roles = [];
}

async function load(page = 0) {
  err.value = "";
  const params = new URLSearchParams({ page: String(page), keyword: keyword.value });
  const data = await apiFetch(`/api/admin/users?${params}`);
  users.value = data.users || [];
  authorities.value = data.authorities || authorities.value;
  Object.assign(pages, data.pages);
}

function search() {
  router.replace({ query: { keyword: keyword.value, page: "0" } });
}

function goPage(p) {
  if (p < 0 || p >= pages.totalPages) return;
  router.replace({ query: { keyword: keyword.value, page: String(p) } });
}

function loadEdit(u) {
  editingUsername.value = u.username;
  form.username = u.username;
  form.password = "";
  form.fullname = u.fullname || "";
  form.email = u.email || "";
  form.photo = u.photo || "";
  form.activated = !!u.activated;
  form.roles = Array.isArray(u.roleNames) ? [...u.roleNames] : [];
}

async function saveUser() {
  err.value = "";
  okMsg.value = "";
  const creating = editingUsername.value == null;
  const payload = {
    username: form.username.trim(),
    fullname: form.fullname,
    email: form.email,
    photo: form.photo,
    activated: form.activated,
    roles: form.roles,
  };
  if (creating && form.password?.trim()) {
    payload.password = form.password.trim();
  }
  try {
    const res = await apiFetch("/api/admin/users/save", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    okMsg.value = res.message || "Đã lưu";
    if (creating) {
      editingUsername.value = form.username.trim();
    }
    await load(pages.number);
  } catch (e) {
    err.value = e.message || "Lỗi lưu";
  }
}

onMounted(() => {
  keyword.value = (route.query.keyword || "").toString();
  const p = parseInt(route.query.page || "0", 10) || 0;
  load(p).catch((e) => {
    err.value = e.message || "Không tải được danh sách";
  });
});

watch(
  () => route.query,
  async (q) => {
    keyword.value = (q.keyword || "").toString();
    const p = parseInt(q.page || "0", 10) || 0;
    try {
      await load(p);
    } catch (e) {
      err.value = e.message || "Không tải được danh sách";
    }
  }
);
</script>
