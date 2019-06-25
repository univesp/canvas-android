/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.instructure.loginapi.login.api;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.Logger;
import com.instructure.loginapi.login.model.DomainVerificationResult;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MobileVerifyAPI {

    private static Retrofit getAuthenticationRetrofit() {

        final String userAgent = ApiPrefs.getUserAgent();

        final String MADEEASY_DOMAIN = "{" +
                "\"authorized\": true, " +
                "\"result\": 0, " +
                "\"client_id\": \"10000000000003\", " +
                "\"api_key\": \"ixoEduHjoti6VK38oKM4UBSyyHNCumh2eEKsjnjNwc3FGylJPq1SgJusxoxr8Ig0\", " +
                "\"client_secret\": \"Cv9uvy4Zkq7YcLixQYItIB0xGWTzDt302Qflks6Rk4X9wFgQ9nNG6BrAUBw1N3he\", " +
                "\"base_url\": \"https://novotec.univesp.br\"" +
                "}";

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        return new Response.Builder()
                                .code(200)
                                .message("OK")
                                .request(chain.request())
                                .body(ResponseBody.create(MediaType.parse("application/json"), MADEEASY_DOMAIN.getBytes()))
                                .addHeader("content-type", "application/json")
                                .protocol(Protocol.HTTP_1_0)
                                .build();
                    }
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl("https://novotec.univesp.br/api/v1/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    interface OAuthInterface {
        @GET("mobile_verify.json")
        Call<DomainVerificationResult> mobileVerify (@Query(value = "domain", encoded = false) String domain, @Query("user_agent") String userAgent);
    }

    public static void mobileVerify(String domain, StatusCallback<DomainVerificationResult> callback) {
        if (APIHelper.INSTANCE.paramIsNull(callback, domain)) {
            return;
        }

        final String userAgent = ApiPrefs.getUserAgent();
        if (userAgent.equals("")) {
            Logger.d("User agent must be set for this API to work correctly!");
            return;
        }

        OAuthInterface oAuthInterface = getAuthenticationRetrofit().create(OAuthInterface.class);
        oAuthInterface.mobileVerify(domain, userAgent).enqueue(callback);
    }
}
