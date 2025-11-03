package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.ReferenceData;
import hr.algebra.cloudbased_inventory_management_system.entity.ReferenceDataType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReferenceDataRepository extends JpaRepository<ReferenceData, Long> {

    List<ReferenceData> findByTypeOrderByValueAsc(ReferenceDataType type);

    List<ReferenceData> findByType(ReferenceDataType type);

    void deleteByType(ReferenceDataType type);
}
