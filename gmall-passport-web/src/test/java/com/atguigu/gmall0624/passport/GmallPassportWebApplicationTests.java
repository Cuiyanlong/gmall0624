package com.atguigu.gmall0624.passport;

import com.atguigu.gmall0624.passport.config.JwtUtil;
import io.jsonwebtoken.impl.TextCodec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void testTWT(){
		//生成token
		String key = "shiliu";
		HashMap<String, Object> map = new HashMap<>();
		map.put("userId","666");
		map.put("nickName","week");
		String salt = "192.168.93.1";
		String token = JwtUtil.encode(key, map, salt);

//		byte[] bytes = TextCodec.BASE64.decode(key+salt);
//		byte[] bytes1 = TextCodec.BASE64.decode(key+"192.168.77.6");


		System.out.println(token);

		System.out.println("---------------------");

		// 解密：
		Map<String, Object> m1 = JwtUtil.decode(token, key, salt);
		System.out.println(m1);
		Map<String, Object> m2 = JwtUtil.decode(token, "atguigu", salt);
		System.out.println(m2);
		Map<String, Object> m3 = JwtUtil.decode(token, key, "192.168.77.6");
		System.out.println(m3);

	}
}
