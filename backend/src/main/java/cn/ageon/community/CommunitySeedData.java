package cn.ageon.community;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CommunitySeedData implements ApplicationRunner {
    private final CommunityQuestionRepository questionRepository;

    public CommunitySeedData(CommunityQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (questionRepository.count() > 0) {
            return;
        }

        CommunityQuestion sse = new CommunityQuestion(
                "Spring Boot 项目中 SSE 连接如何避免超时？",
                "正在实现 AI 流式输出，想了解连接管理和异常重试的常见方案。",
                "Java 后端",
                "CodeLearner"
        );
        sse.addReply(new CommunityReply("李富浩", "可以从心跳、超时配置、断线重连和任务状态恢复四个点拆。"));

        CommunityQuestion rag = new CommunityQuestion(
                "RAG 检索结果相关性不高应该先调哪一层？",
                "文档切片、Embedding 和召回数量之间应该如何排查？",
                "AI 应用",
                "NorthStar"
        );

        CommunityQuestion tcp = new CommunityQuestion(
                "TCP 四次挥手中的 TIME_WAIT 有什么作用？",
                "除了保证最后一个 ACK 到达，还有哪些工程层面的意义？",
                "计算机网络",
                "Packet_01"
        );

        CommunityQuestion redis = new CommunityQuestion(
                "Redis 缓存穿透和缓存击穿如何区分？",
                "希望结合实际业务场景理解两者的解决方案。",
                "数据库",
                "BackendNewbie"
        );
        redis.addReply(new CommunityReply("李富浩", "穿透重点是请求不存在的数据，击穿重点是热点 key 失效。"));

        questionRepository.save(sse);
        questionRepository.save(rag);
        questionRepository.save(tcp);
        questionRepository.save(redis);
    }
}
