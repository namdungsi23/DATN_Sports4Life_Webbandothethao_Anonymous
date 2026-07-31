<template>
  <AdminLayout>
    <AdminReadOnlyNotice />
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
              <button v-if="canWrite" type="button" class="btn btn-sm btn-light" @click="clearForm">+ Tạo mới</button>
            </div>
            <div class="card-body">
              <fieldset :disabled="!canWrite">
              <form @submit.prevent="saveUser">
                <div class="mb-3">
                  <label class="form-label"><strong>Tên đăng nhập <span class="text-danger">*</span></strong></label>
                  <input
                    v-model="form.username"
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': fieldErrors.username }"
                    :readonly="isEditing"
                    @input="delete fieldErrors.username"
                  />
                  <div v-if="fieldErrors.username" class="invalid-feedback d-block">{{ fieldErrors.username }}</div>
                </div>
                <div v-if="!isEditing" class="mb-3">
                  <label class="form-label"><strong>Mật khẩu <span class="text-danger">*</span></strong></label>
                  <input
                    v-model="form.password"
                    type="password"
                    class="form-control"
                    :class="{ 'is-invalid': fieldErrors.password }"
                    placeholder="Tối thiểu 6 ký tự"
                    autocomplete="new-password"
                    @input="delete fieldErrors.password"
                  />
                  <div v-if="fieldErrors.password" class="invalid-feedback d-block">{{ fieldErrors.password }}</div>
                </div>
                <div class="mb-3">
                  <label class="form-label"><strong>Họ tên <span class="text-danger">*</span></strong></label>
                  <input
                    v-model="form.fullname"
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': fieldErrors.fullname }"
                    @input="delete fieldErrors.fullname"
                  />
                  <div v-if="fieldErrors.fullname" class="invalid-feedback d-block">{{ fieldErrors.fullname }}</div>
                </div>
                <div class="mb-3">
                  <label class="form-label"><strong>Email <span class="text-danger">*</span></strong></label>
                  <input
                    v-model="form.email"
                    type="email"
                    class="form-control"
                    :class="{ 'is-invalid': fieldErrors.email }"
                    @input="delete fieldErrors.email"
                  />
                  <div v-if="fieldErrors.email" class="invalid-feedback d-block">{{ fieldErrors.email }}</div>
                </div>
                <div class="mb-3">
                  <label class="form-label"><strong>Ảnh đại diện (Cloudinary)</strong></label>
                  <div class="d-flex align-items-center gap-3 mb-2">
                    <img
                      v-if="form.photo"
                      :src="form.photo"
                      alt="Avatar"
                      class="rounded border"
                      style="width: 64px; height: 64px; object-fit: cover"
                    />
                    <span v-else class="text-muted small">Chưa có ảnh</span>
                  </div>
                  <input
                    type="file"
                    class="form-control mb-2"
                    accept="image/*"
                    :disabled="!canWrite || !isEditing"
                    @change="onAvatarFile"
                  />
                  <input
                    v-model="form.photo"
                    type="text"
                    class="form-control"
                    placeholder="Hoặc dán URL — sẽ đồng bộ lên Cloudinary khi lưu"
                  />
                  <small class="text-muted">Tải file hoặc dán link; hệ thống lưu Cloudinary + SQL (Users.Avatar).</small>
                </div>
                <div v-if="isEditing" class="mb-3">
                  <label class="form-label"><strong>Hạng / Điểm</strong></label>
                  <div class="input-group">
                    <span class="input-group-text">{{ form.rankName || "—" }}</span>
                    <input
                      v-model.number="form.totalPoint"
                      type="number"
                      class="form-control"
                      min="0"
                      :readonly="!canWrite"
                    />
                    <button
                      v-if="canWrite"
                      type="button"
                      class="btn btn-outline-primary"
                      :disabled="pointsSaving"
                      @click="savePoints"
                    >
                      {{ pointsSaving ? "..." : "Cập nhật điểm" }}
                    </button>
                  </div>
                  <small class="text-muted">Đổi điểm sẽ tự đồng bộ hạng theo MinPoint.</small>
                </div>
                <div class="mb-3">
                  <label class="form-label d-block"><strong>Vai trò <span class="text-danger">*</span></strong></label>
                  <div v-for="role in roleOptions" :key="role" class="form-check">
                    <input
                      :id="'role-' + role"
                      class="form-check-input"
                      type="checkbox"
                      :checked="form.roles.includes(role)"
                      @change="toggleRole(role, $event.target.checked); delete fieldErrors.roles"
                    />
                    <label class="form-check-label" :for="'role-' + role">{{ role }}</label>
                  </div>
                  <div v-if="fieldErrors.roles" class="text-danger small mt-1">{{ fieldErrors.roles }}</div>
                </div>
                <div class="form-check mb-3">
                  <input id="act" v-model="form.activated" class="form-check-input" type="checkbox" />
                  <label class="form-check-label" for="act"><strong>Kích hoạt tài khoản</strong></label>
                </div>
                <button type="submit" class="btn btn-success w-100">💾 Lưu</button>
              </form>
              </fieldset>
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
                    <th>Ảnh</th>
                    <th>Username</th>
                    <th>Họ tên</th>
                    <th>Email</th>
                    <th>Hạng</th>
                    <th>Điểm</th>
                    <th>Vai trò</th>
                    <th>Trạng thái</th>
                    <th class="text-center">Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="u in users" :key="u.username">
                    <td>
                      <img
                        v-if="u.photo || u.avatar"
                        :src="u.photo || u.avatar"
                        alt=""
                        class="rounded"
                        style="width: 40px; height: 40px; object-fit: cover"
                      />
                      <span v-else class="text-muted">—</span>
                    </td>
                    <td>{{ u.username }}</td>
                    <td>{{ u.fullname }}</td>
                    <td>{{ u.email }}</td>
                    <td>
                      <span v-if="u.rankName" class="badge text-bg-light border">{{ u.rankName }}</span>
                      <span v-else class="text-muted">—</span>
                    </td>
                    <td>{{ u.totalPoint ?? 0 }}</td>
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
                      <button v-if="canWrite" type="button" class="btn btn-sm btn-warning" @click="loadEdit(u)">✏️</button>
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
import AdminReadOnlyNotice from "../../components/admin/AdminReadOnlyNotice.vue";
import { apiFetch } from "../../services/http.js";
import { useAppStore } from "../../stores/appStore";
import { userCanWriteCatalog } from "../../utils/adminAccess";
import { firstError, getApiError, runValidation } from "../../utils/validators";

const store = useAppStore();
const canWrite = computed(() => userCanWriteCatalog(store.state.user));

const route = useRoute();
const router = useRouter();
const err = ref("");
const okMsg = ref("");
const fieldErrors = reactive({});
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
  accountId: null,
  rankName: "",
  totalPoint: 0,
});
const editingUsername = ref(null);
const pointsSaving = ref(false);
const avatarUploading = ref(false);

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
  form.accountId = null;
  form.rankName = "";
  form.totalPoint = 0;
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
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
  form.accountId = u.id ?? null;
  form.rankName = u.rankName || "";
  form.totalPoint = u.totalPoint ?? 0;
}

async function onAvatarFile(e) {
  const file = e.target.files?.[0];
  e.target.value = "";
  if (!file || !form.username) return;
  if (!isEditing.value) {
    err.value = "Chọn người dùng trước khi tải ảnh.";
    return;
  }
  avatarUploading.value = true;
  err.value = "";
  okMsg.value = "";
  try {
    const fd = new FormData();
    fd.append("file", file);
    const res = await apiFetch(`/api/admin/users/${encodeURIComponent(form.username)}/avatar`, {
      method: "POST",
      body: fd,
    });
    form.photo = res.photo || res.user?.photo || form.photo;
    okMsg.value = res.message || "Đã upload ảnh lên Cloudinary.";
    await load(pages.number);
  } catch (ex) {
    err.value = ex?.message || "Upload ảnh thất bại.";
  } finally {
    avatarUploading.value = false;
  }
}

async function savePoints() {
  if (!form.accountId) {
    err.value = "Không xác định được tài khoản.";
    return;
  }
  pointsSaving.value = true;
  err.value = "";
  okMsg.value = "";
  try {
    const res = await apiFetch(`/api/admin/users/${form.accountId}/points`, {
      method: "POST",
      body: JSON.stringify({ totalPoint: Number(form.totalPoint) || 0 }),
    });
    okMsg.value = res.message || "Đã cập nhật điểm.";
    if (res.user) {
      form.rankName = res.user.rankName || "";
      form.totalPoint = res.user.totalPoint ?? 0;
    }
    await load(pages.number);
  } catch (e) {
    err.value = e?.message || "Cập nhật điểm thất bại.";
  } finally {
    pointsSaving.value = false;
  }
}

async function saveUser() {
  err.value = "";
  okMsg.value = "";
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
  const creating = editingUsername.value == null;

  const schema = {
    username: ["required", "username"],
    fullname: [
      "required",
      { type: "min", min: 2, message: "Họ tên tối thiểu 2 ký tự." },
      { type: "max", max: 100 },
    ],
    email: ["required", "email", { type: "max", max: 100 }],
  };
  if (creating) {
    schema.password = [
      { type: "required", message: "Mật khẩu là bắt buộc khi tạo tài khoản." },
      { type: "min", min: 6, message: "Mật khẩu tối thiểu 6 ký tự." },
    ];
  } else if (String(form.password || "").trim()) {
    schema.password = [{ type: "min", min: 6, message: "Mật khẩu mới tối thiểu 6 ký tự." }];
  }

  const result = runValidation(
    {
      username: form.username,
      fullname: form.fullname,
      email: form.email,
      password: form.password,
    },
    schema
  );
  if (!result.ok) {
    Object.assign(fieldErrors, result.errors);
    err.value = firstError(result.errors);
    return;
  }
  if (!Array.isArray(form.roles) || !form.roles.length) {
    fieldErrors.roles = "Vui lòng chọn ít nhất một vai trò.";
    err.value = fieldErrors.roles;
    return;
  }

  const payload = {
    username: String(result.values.username),
    fullname: String(result.values.fullname),
    email: String(result.values.email),
    photo: form.photo,
    activated: form.activated,
    roles: form.roles,
  };
  if (form.password?.trim()) {
    payload.password = form.password.trim();
  }
  try {
    const res = await apiFetch("/api/admin/users/save", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    okMsg.value = res.message || "Đã lưu";
    if (creating) {
      editingUsername.value = payload.username;
    }
    await load(pages.number);
  } catch (e) {
    const api = getApiError(e, "Lỗi lưu");
    Object.assign(fieldErrors, api.errors || {});
    err.value = api.message;
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
