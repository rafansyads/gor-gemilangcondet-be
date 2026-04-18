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

    @Query(value = """
            select * from pos_products p
            where (cast(:category as varchar) is null or p.category = :category)
              and (cast(:query as varchar) is null
                   or lower(p.name) like lower('%' || cast(:query as varchar) || '%')
                   or lower(p.sku) like lower('%' || cast(:query as varchar) || '%'))
            order by p.name asc
            """, nativeQuery = true)
    List<PosProduct> search(@Param("query") String query,
                            @Param("category") String category);
}
