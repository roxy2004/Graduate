import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {path:'/',redirect:'/login'},
    {
      path:'/manager',
      component:()=>import('@/views/Manager.vue'),
      redirect:'/manager/home',
      children:[
        {path:'home',component:()=>import('@/views/manager/Home.vue')},
      ]
    },
    {path:'/login',component:()=>import('@/views/Login.vue')},
  ]
})

export default router