package com.atguigu.gmall0624.passport.config;

import io.jsonwebtoken.*;

import java.util.Map;

public class JwtUtil {
    /**
     * 生成token 的方法
     * @param key 固定的字符串
     * @param param  存储用户信息
     * @param salt 服务器的IP地址
     * @return
     */
    public static String encode(String key,Map<String,Object> param,String salt){
        // key = key + salt
        if(salt!=null){
            key+=salt;
        }
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256,key);
        // 用户信息
        jwtBuilder = jwtBuilder.setClaims(param);
        // 生成token
        String token = jwtBuilder.compact();
        return token;

    }

    /**
     * 解密token
     * @param token
     * @param key
     * @param salt
     * @return
     */
    public  static Map<String,Object> decode(String token , String key, String salt){
        Claims claims=null;
        if (salt!=null){
            key+=salt;
        }
        try {
            // 获取token 的主体
            claims= Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        } catch ( JwtException e) {
            return null;
        }
        return  claims;
    }

}
