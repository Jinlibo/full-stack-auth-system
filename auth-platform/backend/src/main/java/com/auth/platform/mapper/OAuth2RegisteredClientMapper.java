package com.auth.platform.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface OAuth2RegisteredClientMapper {

    @Select("SELECT client_secret FROM oauth2_registered_client WHERE client_id = #{clientId}")
    String selectSecretByClientId(@Param("clientId") String clientId);

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

    @Update("UPDATE oauth2_registered_client SET client_secret = #{clientSecret} WHERE client_id = #{clientId}")
    void updateSecret(@Param("clientSecret") String clientSecret, @Param("clientId") String clientId);

    @Update("UPDATE oauth2_registered_client SET redirect_uris = #{redirectUris}, client_name = #{clientName} WHERE client_id = #{clientId}")
    void updateRedirectUris(@Param("redirectUris") String redirectUris,
                            @Param("clientName") String clientName,
                            @Param("clientId") String clientId);

    @Delete("DELETE FROM oauth2_registered_client WHERE client_id = #{clientId}")
    void deleteByClientId(@Param("clientId") String clientId);
}
