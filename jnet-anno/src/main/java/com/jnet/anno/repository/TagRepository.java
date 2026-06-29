package com.jnet.anno.repository;

import com.jnet.anno.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 标签数据访问层
 *
 * @author mu
 * @version 1.0
 * @since 2026/6/29
 */
@Repository
@Transactional
public interface TagRepository extends JpaRepository<Tag, Long> {
}
