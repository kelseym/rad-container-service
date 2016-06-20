package org.nrg.execution.services;

import org.nrg.execution.daos.ScriptEnvironmentDao;
import org.nrg.execution.model.ScriptEnvironment;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateScriptEnvironmentService
        extends AbstractHibernateEntityService<ScriptEnvironment, ScriptEnvironmentDao>
        implements ScriptEnvironmentService {

//    @Override
//    @Transactional
//    public ScriptEnvironment create(final ScriptEnvironment scriptEnvironment) {
//        if (scriptEnvironment.getDockerImage() != null) {
//            final DockerImage image = dockerImageService.retrieve(scriptEnvironment.getDockerImage().getId());
//            if (image != null) {
//                scriptEnvironment.setDockerImage(image);
//            }
//        }
//        return super.create(scriptEnvironment);
//    }
}
