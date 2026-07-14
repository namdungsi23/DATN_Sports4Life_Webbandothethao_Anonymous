package poly.edu.ASSM.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Addresses;

@Repository
public interface AddressRepository extends JpaRepository<Addresses, Integer> {

    List<Addresses> findByAccount_IdOrderByIsDefaultDescCreatedAtDesc(Long accountId);

    Optional<Addresses> findByIdAndAccount_Id(Integer id, Long accountId);

    @Modifying
    @Query("UPDATE Addresses a SET a.isDefault = false, a.updatedAt = CURRENT_TIMESTAMP WHERE a.account.id = :accountId")
    void clearDefaultForAccount(@Param("accountId") Long accountId);

    long countByAccount_Id(Long accountId);
}
