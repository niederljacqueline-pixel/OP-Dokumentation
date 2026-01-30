package at.fh.opdoc.repo;

import at.fh.opdoc.model.OpDokumentation;
import java.util.List;

public interface OpDokumentationRepository {

    List<OpDokumentation> findAll();

    OpDokumentation findByOpPlanId(Integer opPlanId);


    List<OpDokumentation> findByPatientId(Integer patientId);

    OpDokumentation save(OpDokumentation doku);

    void deleteById(Integer id);
}
