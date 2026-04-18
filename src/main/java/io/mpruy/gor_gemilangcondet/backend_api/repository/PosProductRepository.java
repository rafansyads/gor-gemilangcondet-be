package io.mpruy.gor_gemilangcondet.backend_api.repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.pos.PosProduct;
import io.mpruy.gor_gemilangcondet.backend_api.entities.pos.PosProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PosProductRepository extends JpaRepository<PosProduct, UUID> {

    boolean existsBySkuIgnoreCase(String sku);

    @Query("""
            select p from PosProduct p
            where (:category is null or p.category = :category)
              and (:query is null or trim(:query) = ''
                   or lower(p.name) like lower(concat('%', :query, '%'))
                   or lower(p.sku) like lower(concat('%', :query, '%')))
            order by p.name asc
            """)
    List<PosProduct> search(@Param("query") String query,
                            @Param("category") PosProductCategory category);
}
