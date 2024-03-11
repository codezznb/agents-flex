/*
 *  Copyright (c) 2022-2023, Agents-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.agentsflex.llm.spark;

import com.agentsflex.message.MessageStatus;
import com.agentsflex.parser.AiMessageParser;
import com.agentsflex.parser.FunctionMessageParser;
import com.agentsflex.parser.impl.BaseAiMessageParser;
import com.agentsflex.parser.impl.BaseFunctionMessageParser;
import com.agentsflex.prompt.DefaultPromptFormat;
import com.agentsflex.prompt.FunctionPrompt;
import com.agentsflex.prompt.Prompt;
import com.agentsflex.prompt.PromptFormat;
import com.agentsflex.util.HashUtil;
import com.agentsflex.util.Maps;
import com.alibaba.fastjson.JSON;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class SparkLlmUtil {

    private static final PromptFormat promptFormat = new DefaultPromptFormat();

    public static AiMessageParser getAiMessageParser() {
        BaseAiMessageParser aiMessageParser = new BaseAiMessageParser();
        aiMessageParser.setContentPath("$.payload.choices.text[0].content");
        aiMessageParser.setIndexPath("$.payload.choices.text[0].index");
        aiMessageParser.setStatusPath("$.payload.choices.status");
        aiMessageParser.setStatusParser(content -> parseMessageStatus((Integer) content));
        return aiMessageParser;
    }


    public static FunctionMessageParser getFunctionMessageParser() {
        BaseFunctionMessageParser functionMessageParser = new BaseFunctionMessageParser();
        functionMessageParser.setFunctionNamePath("$.payload.choices.text[0].function_call.name");
        functionMessageParser.setFunctionArgsPath("$.payload.choices.text[0].function_call.arguments");
        functionMessageParser.setFunctionArgsParser(JSON::parseObject);
        return functionMessageParser;
    }


    public static String promptToPayload(Prompt prompt, SparkLlmConfig config) {
        // https://www.xfyun.cn/doc/spark/Web.html#_1-%E6%8E%A5%E5%8F%A3%E8%AF%B4%E6%98%8E
        Maps.Builder root = Maps.of("header", Maps.of("app_id", config.getAppId()).put("uid", UUID.randomUUID()));
        root.put("parameter", Maps.of("chat", Maps.of("domain", "generalv3").put("temperature", 0.5).put("max_tokens", 1024)));
        root.put("payload", Maps.of("message", Maps.of("text", promptFormat.toMessagesJsonKey(prompt)))
            .putIfNotEmpty("functions", Maps.ofNotNull("text", promptFormat.toFunctionsJsonKey((FunctionPrompt) prompt)))
        );
        return JSON.toJSONString(root.build());
    }

    public static MessageStatus parseMessageStatus(Integer status) {
        switch (status) {
            case 0:
                return MessageStatus.START;
            case 1:
                return MessageStatus.MIDDLE;
            case 2:
                return MessageStatus.END;
        }
        return MessageStatus.UNKNOW;
    }

    public static String createURL(SparkLlmConfig config) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss '+0000'", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date());

        String header = "host: spark-api.xf-yun.com\n";
        header += "date: " + date + "\n";
        header += "GET /" + config.getVersion() + "/chat HTTP/1.1";

        String base64 = HashUtil.hmacSHA256ToBase64(header, config.getApiSecret());
        String authorization_origin = "api_key=\"" + config.getApiKey()
            + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + base64 + "\"";

        String authorization = Base64.getEncoder().encodeToString(authorization_origin.getBytes());
        return "ws://spark-api.xf-yun.com/" + config.getVersion() + "/chat?authorization=" + authorization
            + "&date=" + urlEncode(date) + "&host=spark-api.xf-yun.com";
    }

    private static String urlEncode(String content) {
        try {
            return URLEncoder.encode(content, "utf-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
