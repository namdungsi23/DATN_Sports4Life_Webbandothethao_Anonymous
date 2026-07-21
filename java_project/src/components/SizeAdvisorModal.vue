<script setup>
import { computed, reactive, ref, watch } from "vue";
import {
  GENDER_OPTIONS,
  HEIGHT_OPTIONS,
  WEIGHT_OPTIONS,
  adviseSize,
} from "../utils/sizeAdvisor";

const props = defineProps({
  open: { type: Boolean, default: false },
  sizes: { type: Array, default: () => [] },
  categoryName: { type: String, default: "" },
});

const emit = defineEmits(["close", "apply"]);

const form = reactive({
  gender: "male",
  height: null,
  weight: null,
});

const result = ref(null);
const error = ref("");

watch(
  () => props.open,
  (v) => {
    if (v) {
      result.value = null;
      error.value = "";
    }
  }
);

const canSubmit = computed(() => form.height != null && form.weight != null);

const onSubmit = () => {
  error.value = "";
  if (!canSubmit.value) {
    error.value = "Vui lòng chọn chiều cao và cân nặng.";
    return;
  }
  result.value = adviseSize({
    gender: form.gender,
    height: Number(form.height),
    weight: Number(form.weight),
    sizes: props.sizes,
    categoryName: props.categoryName,
  });
};

const applyResult = () => {
  if (result.value?.matched) {
    emit("apply", result.value.matched);
    emit("close");
  }
};
</script>

<template>
  <Transition name="size-advisor">
    <div v-if="open" class="size-advisor" @click.self="emit('close')">
      <div class="size-advisor__box" role="dialog" aria-label="Hướng dẫn chọn size">
        <header class="size-advisor__head">
          <h3>Hướng dẫn chọn size</h3>
          <button type="button" class="size-advisor__close" aria-label="Đóng" @click="emit('close')">×</button>
        </header>

        <p class="size-advisor__desc">
          Nhập thông tin của bạn, hệ thống sẽ gợi ý size phù hợp nhất.
        </p>

        <div class="size-advisor__grid">
          <label class="size-advisor__field">
            <span>Giới tính</span>
            <select v-model="form.gender">
              <option v-for="opt in GENDER_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </label>

          <label class="size-advisor__field">
            <span>Chiều cao</span>
            <select v-model="form.height">
              <option :value="null" disabled>-- Chọn chiều cao --</option>
              <option v-for="opt in HEIGHT_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </label>

          <label class="size-advisor__field">
            <span>Cân nặng</span>
            <select v-model="form.weight">
              <option :value="null" disabled>-- Chọn cân nặng --</option>
              <option v-for="opt in WEIGHT_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </label>
        </div>

        <p v-if="error" class="size-advisor__error">{{ error }}</p>

        <button type="button" class="size-advisor__submit" @click="onSubmit">
          Gợi ý size cho tôi
        </button>

        <div v-if="result" class="size-advisor__result">
          <p class="size-advisor__result-size">
            Size gợi ý: <strong>{{ result.matched || result.recommended }}</strong>
          </p>
          <p class="size-advisor__result-note">{{ result.note }}</p>
          <button
            v-if="result.matched"
            type="button"
            class="size-advisor__apply"
            @click="applyResult"
          >
            Chọn size {{ result.matched }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.size-advisor {
  position: fixed;
  inset: 0;
  z-index: 1300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.45);
}

.size-advisor__box {
  width: min(440px, 100%);
  background: #fff;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
}

.size-advisor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.size-advisor__head h3 {
  margin: 0;
  font-size: 19px;
  color: #1a3c34;
}

.size-advisor__close {
  border: none;
  background: none;
  font-size: 26px;
  line-height: 1;
  color: #888;
  cursor: pointer;
}

.size-advisor__desc {
  margin: 0 0 16px;
  font-size: 14px;
  color: #666;
}

.size-advisor__grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.size-advisor__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.size-advisor__field span {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.size-advisor__field select {
  height: 42px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  outline: none;
  background: #fff;
  cursor: pointer;
}

.size-advisor__field select:focus {
  border-color: #1a3c34;
  box-shadow: 0 0 0 3px rgba(26, 60, 52, 0.12);
}

.size-advisor__error {
  margin: 12px 0 0;
  color: #dc2626;
  font-size: 13px;
}

.size-advisor__submit {
  width: 100%;
  margin-top: 18px;
  height: 46px;
  border: none;
  border-radius: 8px;
  background: #1a3c34;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.size-advisor__submit:hover {
  background: #144038;
}

.size-advisor__result {
  margin-top: 18px;
  padding: 16px;
  border-radius: 10px;
  background: #f0f7f4;
  border: 1px solid #d6e0dc;
  text-align: center;
}

.size-advisor__result-size {
  margin: 0 0 6px;
  font-size: 16px;
  color: #1a3c34;
}

.size-advisor__result-size strong {
  font-size: 22px;
}

.size-advisor__result-note {
  margin: 0 0 12px;
  font-size: 13px;
  color: #5a6b64;
}

.size-advisor__apply {
  border: none;
  border-radius: 8px;
  background: #e8871e;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  padding: 10px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.size-advisor__apply:hover {
  background: #d07615;
}

.size-advisor-enter-active,
.size-advisor-leave-active {
  transition: opacity 0.2s ease;
}

.size-advisor-enter-from,
.size-advisor-leave-to {
  opacity: 0;
}
</style>
