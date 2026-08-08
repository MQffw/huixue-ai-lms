import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/index',
    children: [
      { path: '/index', component: () => import('@/views/index/index.vue') },
      { path: '/clazz', component: () => import('@/views/clazz/index.vue') },
      { path: '/stu', component: () => import('@/views/stu/index.vue') },
      { path: '/dept', component: () => import('@/views/dept/index.vue') },
      { path: '/emp', component: () => import('@/views/emp/index.vue') },
      { path: '/course', component: () => import('@/views/course/index.vue') },
      { path: '/attendance', component: () => import('@/views/attendance/index.vue') },
      { path: '/score', component: () => import('@/views/score/index.vue') },
      { path: '/payment', component: () => import('@/views/payment/index.vue') },
      { path: '/employment', component: () => import('@/views/employment/index.vue') },
      { path: '/notice', component: () => import('@/views/notice/index.vue') },
      { path: '/violation', component: () => import('@/views/violation/index.vue') },
      { path: '/report/emp', component: () => import('@/views/report/emp/index.vue') },
      { path: '/report/stu', component: () => import('@/views/report/stu/index.vue') },
      { path: '/log', component: () => import('@/views/log/index.vue') },
      { path: '/ai-stats', component: () => import('@/views/aiStats/index.vue') },
      { path: '/ai', component: () => import('@/views/ai/index.vue') }
    ]
  },
  { path: '/login', component: () => import('@/views/login/index.vue') }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 全局路由守卫 - 登录验证
router.beforeEach((to, from, next) => {
  // 登录页面直接放行
  if (to.path === '/login') {
    next();
    return;
  }

  // 检查本地存储中是否有登录信息
  const loginUser = localStorage.getItem('loginUser');

  if (loginUser) {
    // 已登录，放行
    next();
  } else {
    // 未登录，跳转到登录页
    next('/login');
  }
});

export default router;
