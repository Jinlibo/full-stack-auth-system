package com.auth.platform.mapper;

import org.apache.ibatis.annotations.*;

/**
 * OAuth2 注册客户端 Mapper 接口
 * <p>直接操作 Spring Authorization Server 的内置表 oauth2_registered_client，
 * 提供客户端的增删改查操作。该表结构特殊（含 JSON 格式的 client_settings 和 token_settings），
 * 故使用原生 MyBatis 注解 SQL 而非 MyBatis-Plus 封装。</p>
 */
@Mapper
public interface OAuth2RegisteredClientMapper {

    /**
     * 根据 client_id 查询 client_secret（BCrypt 加密后的密文）
     *
     * @param clientId OAuth2 客户端 ID
     * @return BCrypt 加密后的 client_secret，不存在则返回 null
     */
    @Select("SELECT client_secret FROM oauth2_registered_client WHERE client_id = #{clientId}")
    String selectSecretByClientId(@Param("clientId") String clientId);

    /**
     * 向 oauth2_registered_client 表插入一条新的客户端注册记录
     *
     * @param id                          记录唯一 ID（UUID 格式）
     * @param clientId                    OAuth2 客户端 ID（即产品的 productKey）
     * @param clientSecret                BCrypt 加密后的客户端密钥
     * @param clientName                  客户端名称（即产品名称）
     * @param clientAuthenticationMethods 支持的客户端认证方式，逗号分隔
     * @param authorizationGrantTypes     支持的授权类型，逗号分隔
     * @param redirectUris                OAuth2 授权码回调地址，逗号分隔
     * @param scopes                      授权范围，逗号分隔
     * @param clientSettings              客户端设置（JSON 格式字符串）
     * @param tokenSettings               令牌设置（JSON 格式字符串）
     */
    @Insert("INSERT INTO oauth2_registered_client " +
            "(id, client_id, client_secret, client_name, " +
            "client_authentication_methods, authorization_grant_types, " +
            "redirect_uris, scopes, client_settings, token_settings) " +
            "VALUES (#{id}, #{clientId}, #{clientSecret}, #{clientName}, " +
            "#{clientAuthenticationMethods}, #{authorizationGrantTypes}, " +
            "#{redirectUris}, #{scopes}, #{clientSettings}, #{tokenSettings})")
    void insert(@Param("id") String id,
                @Param("clientId") String clientId,
                @Param("clientSecret") String clientSecret,
                @Param("clientName") String clientName,
                @Param("clientAuthenticationMethods") String clientAuthenticationMethods,
                @Param("authorizationGrantTypes") String authorizationGrantTypes,
                @Param("redirectUris") String redirectUris,
                @Param("scopes") String scopes,
                @Param("clientSettings") String clientSettings,
                @Param("tokenSettings") String tokenSettings);

    /**
     * 根据 client_id 更新客户端密钥
     *
     * @param clientSecret 新的 BCrypt 加密后的客户端密钥
     * @param clientId     OAuth2 客户端 ID
     */
    @Update("UPDATE oauth2_registered_client SET client_secret = #{clientSecret} WHERE client_id = #{clientId}")
    void updateSecret(@Param("clientSecret") String clientSecret, @Param("clientId") String clientId);

    /**
     * 根据 client_id 更新回调地址列表和客户端名称
     *
     * @param redirectUris 新的回调地址，逗号分隔字符串
     * @param clientName   新的客户端名称（产品名称）
     * @param clientId     OAuth2 客户端 ID
     */
    @Update("UPDATE oauth2_registered_client SET redirect_uris = #{redirectUris}, client_name = #{clientName} WHERE client_id = #{clientId}")
    void updateRedirectUris(@Param("redirectUris") String redirectUris,
                            @Param("clientName") String clientName,
                            @Param("clientId") String clientId);

    /**
     * 根据 client_id 删除客户端注册记录
     * <p>客户端注销后，使用该 client_id 发起的 OAuth2 授权请求将被 Spring Auth Server 拒绝。</p>
     *
     * @param clientId OAuth2 客户端 ID
     */
    @Delete("DELETE FROM oauth2_registered_client WHERE client_id = #{clientId}")
    void deleteByClientId(@Param("clientId") String clientId);
}
