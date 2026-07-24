package cn.ageon.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommunityQuestionRepository extends JpaRepository<CommunityQuestion, Long>, JpaSpecificationExecutor<CommunityQuestion> {
}
