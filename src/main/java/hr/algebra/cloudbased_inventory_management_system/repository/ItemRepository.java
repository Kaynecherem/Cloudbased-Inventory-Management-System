package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByNameIgnoreCaseAndUnitIgnoreCaseAndIsActiveTrue(String name, String unit);

    boolean existsByNameIgnoreCaseAndUnitIgnoreCaseAndIsActiveTrueAndIdNot(String name, String unit, Long id);

    Optional<Item> findByIdAndIsActiveTrue(Long id);

    boolean existsByIdAndIsActiveTrue(Long id);

    @Query("SELECT DISTINCT TRIM(i.unit) FROM Item i WHERE i.unit IS NOT NULL AND i.unit <> ''")
    List<String> findDistinctUnits();

    @Query("SELECT DISTINCT TRIM(i.category) FROM Item i WHERE i.category IS NOT NULL AND i.category <> ''")
    List<String> findDistinctCategories();
}
