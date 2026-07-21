<template>
  <div class="address-form-fields">
    <div class="row g-3">
      <template v-if="showReceiver">
        <div class="col-md-6">
          <label class="form-label">Họ và tên người nhận <span class="text-danger">*</span></label>
          <input
            v-model="model.receiverName"
            type="text"
            class="form-control"
            :class="{ 'is-invalid': formErrors.receiverName }"
            placeholder="Nguyễn Văn A"
          />
          <div v-if="formErrors.receiverName" class="invalid-feedback d-block">{{ formErrors.receiverName }}</div>
        </div>
        <div class="col-md-6">
          <label class="form-label">Số điện thoại người nhận <span class="text-danger">*</span></label>
          <input
            v-model="model.receiverPhone"
            type="tel"
            class="form-control"
            :class="{ 'is-invalid': formErrors.receiverPhone }"
            placeholder="09xxxxxxxx"
          />
          <div v-if="formErrors.receiverPhone" class="invalid-feedback d-block">{{ formErrors.receiverPhone }}</div>
        </div>
      </template>

      <div class="col-md-6">
        <label class="form-label">Thành phố <span class="text-danger">*</span></label>
        <input
          v-model="model.province"
          type="text"
          class="form-control"
          :class="{ 'is-invalid': formErrors.province }"
          placeholder="TP. Hồ Chí Minh"
        />
        <div v-if="formErrors.province" class="invalid-feedback d-block">{{ formErrors.province }}</div>
      </div>
      <div class="col-md-6">
        <label class="form-label">Phường <span class="text-danger">*</span></label>
        <input
          v-model="model.ward"
          type="text"
          class="form-control"
          :class="{ 'is-invalid': formErrors.ward }"
          placeholder="Phường / Xã"
        />
        <div v-if="formErrors.ward" class="invalid-feedback d-block">{{ formErrors.ward }}</div>
      </div>
      <div class="col-12">
        <label class="form-label">Địa chỉ <span class="text-danger">*</span></label>
        <input
          v-model="model.addressDetail"
          type="text"
          class="form-control"
          :class="{ 'is-invalid': formErrors.addressDetail }"
          placeholder="Số nhà, tên đường"
        />
        <div v-if="formErrors.addressDetail" class="invalid-feedback d-block">{{ formErrors.addressDetail }}</div>
      </div>
      <div class="col-12" v-if="showLabel">
        <label class="form-label">Nhãn địa chỉ</label>
        <input
          v-model="model.label"
          type="text"
          class="form-control"
          :disabled="model.isDefault"
          placeholder="Ví dụ: Nhà riêng, Công ty..."
        />
        <div class="form-text">Nếu đặt làm mặc định, nhãn sẽ tự động là "Địa chỉ mặc định".</div>
      </div>
      <div class="col-12" v-if="showDefaultCheckbox">
        <div class="form-check">
          <input id="addr-is-default" v-model="model.isDefault" class="form-check-input" type="checkbox" />
          <label class="form-check-label" for="addr-is-default">Đặt làm địa chỉ mặc định</label>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  formErrors: { type: Object, default: () => ({}) },
  showLabel: { type: Boolean, default: true },
  showDefaultCheckbox: { type: Boolean, default: true },
  showReceiver: { type: Boolean, default: false },
});

const model = defineModel({ type: Object, required: true });
</script>
