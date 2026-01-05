package com.github.paicoding.forum.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.Test;

public class DockerSandboxTest {

    @Test
    public void testDockerConnection() {
        // Mac 默认的 Docker 连接地址 (Unix Socket)
        String dockerHost = "unix:///var/run/docker.sock";

        System.out.println("⏳ 正在尝试连接 Docker...");

        try {
            // 1. 创建客户端
            DockerClient dockerClient = DockerClientBuilder.getInstance(dockerHost).build();

            // 2. Ping 测试
            PingCmd pingCmd = dockerClient.pingCmd();
            pingCmd.exec();

            System.out.println("✅ Docker 连接成功！");

            // 3. 打印版本信息
            System.out.println("🐳 Docker 版本: " + dockerClient.versionCmd().exec().getVersion());

        } catch (Exception e) {
            System.err.println("❌ Docker 连接失败！请检查 Docker Desktop 是否启动。");
            e.printStackTrace();
        }
    }
}