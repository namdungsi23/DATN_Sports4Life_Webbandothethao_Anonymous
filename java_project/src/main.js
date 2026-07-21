import { createApp } from 'vue'
import './style.css'
import './assets/css/design-system.css'
import App from './App.vue'
import router from './router/index.js'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
//CSS
import './assets/css/header.css'
import './assets/css/footer.css'
import './assets/css/product.css'
import './assets/css/product-detail.css'
import './assets/css/login.css'
import './assets/css/home.css'
import './assets/css/cart.css'
import './assets/css/checkout.css'
import './assets/css/contact.css'
import './assets/css/profile.css'
import './assets/css/favorites.css'
import './assets/css/admin.css'
import './assets/css/chat.css'

createApp(App).use(router).mount('#app')

console.log("import.meta", import.meta);
console.log("env", import.meta.env);