package at.fh.opdoc.repo;

import at.fh.opdoc.model.OpPlan;
import java.util.List;

public interface OpPlanRepository {

    List<OpPlan> findAll();

    List<OpPlan> findByPatientId(Integer patientId);

    OpPlan save(OpPlan plan);

    void deleteById(Integer id);
}


