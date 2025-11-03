package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.ItemActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemActivityRepository extends JpaRepository<ItemActivity, Long> {

    Page<ItemActivity> findByItemIdOrderByCreatedAtDesc(Long itemId, Pageable pageable);
}
