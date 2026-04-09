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
        {path:'teacher/users',component:()=>import('@/views/manager/TeacherUsers.vue')},
        {path:'teacher/questions',component:()=>import('@/views/manager/TeacherQuestions.vue')},
      ]
    },
    {path:'/login',component:()=>import('@/views/Login.vue')},
  ]
})

router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  if (to.path === '/login') {
    next()
    return
  }
  if (!user) {
    next('/login')
    return
  }
  if (to.path.startsWith('/manager/teacher') && user.role !== 'teacher') {
    next('/manager/home')
    return
  }
  next()
})

export default router