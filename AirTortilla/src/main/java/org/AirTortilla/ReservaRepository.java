package org.AirTortilla;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<ReservaEntity, Long> {
    List<ReservaEntity> findAllByOrderByIdAsc();

    List<ReservaEntity> findByDestinoNombreIgnoreCaseOrderByIdAsc(String destino);

    Optional<ReservaEntity> findByLocalizador(String localizador);

    boolean existsByLocalizador(String localizador);
}
