package poly.edu.ASSM.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.ASSM.Entity.Ranks;

public interface RankRepository extends JpaRepository<Ranks, Integer> {
}
