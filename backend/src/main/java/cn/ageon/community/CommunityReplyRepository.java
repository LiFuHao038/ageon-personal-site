package cn.ageon.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityReplyRepository extends JpaRepository<CommunityReply, Long> {
    List<CommunityReply> findAllByOrderByCreatedAtDesc();
}
