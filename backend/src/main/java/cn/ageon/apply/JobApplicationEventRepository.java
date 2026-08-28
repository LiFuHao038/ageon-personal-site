package cn.ageon.apply;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationEventRepository extends JpaRepository<JobApplicationEvent, Long> {

    List<JobApplicationEvent> findByApplicationIdOrderByOccurredAtAsc(Long applicationId);

    /** 某用户全部投递的全部事件，供统计在内存中聚合（个人站数据量小，避免 N+1）。 */
    List<JobApplicationEvent> findByApplicationOwnerIdOrderByOccurredAtAsc(Long ownerId);
}
