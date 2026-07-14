package poly.edu.ASSM.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.ASSM.entity.Ranks;

public interface RankRepository extends JpaRepository<Ranks, Integer> {
}
