package com.agentsflex.llm.qwen.test;

import com.agentsflex.llm.Llm;
import com.agentsflex.llm.qwen.QwenLlm;
import com.agentsflex.llm.qwen.QwenLlmConfig;
import com.agentsflex.prompt.SimplePrompt;

public class QwenTest {

    public static void main(String[] args) throws InterruptedException {
        QwenLlmConfig config = new QwenLlmConfig();
        config.setApiKey("sk-28a6be3236****");
        config.setModel("qwen-turbo");

        Llm llm = new QwenLlm(config);
        llm.chat(new SimplePrompt("请写一个小兔子战胜大灰狼的故事"), (llm1, aiMessage) -> {
            System.out.println(">>>>" + aiMessage.getContent());
        });

        Thread.sleep(10000);
    }
}