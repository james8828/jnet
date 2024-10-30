<template>
  <div>
    <form @submit.prevent="handleSubmit">
      <input type="text" v-model="username" placeholder="Username"/>
      <input type="password" v-model="password" placeholder="Password"/>
      <button type="submit">Login</button>
    </form>
  </div>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import serviceAxios from '@utils/serviceAxios';
import router from '@router';
import {ElMessage} from 'element-plus'


export default defineComponent({

  name: 'LoginPage',
  setup() {
    const username = ref('admin');
    const password = ref('123456');

    const handleSubmit = async () => {
      // 登录逻辑
      try {
        localStorage.clear()
        const login = serviceAxios.post(process.env.AUTH_SERVICE_URL +'/oauth2/authorize', 'grant_type=password&username=' + username.value + '&password=' + password.value + '&client_id=front-client',
            {headers: {'Content-Type': 'application/x-www-form-urlencoded'}});
        login.then(
            (res:any) => {
              if (res.code === 200) {
                ElMessage({
                  message: res.msg,
                  type: 'success',
                })
                // 将token存储到本地存储中
                localStorage.setItem('access_token', res.data.access_token);
                localStorage.setItem('token_type', res.data.token_type);
                localStorage.setItem('expires_in', res.data.expires_in);
                // 登录成功后，重定向到首页
                router.push({name: 'Home'});
              } else {
                ElMessage.error(res.msg)
              }
            },
            (err:any) => {
              ElMessage.error(`Login failed: ${err}`)
            })
      } catch (error) {
        ElMessage.error(`Login failed: ${error}`)
      }
    };

    return {
      username,
      password,
      handleSubmit,
    };
  },
});
</script>