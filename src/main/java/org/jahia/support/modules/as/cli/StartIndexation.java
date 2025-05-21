package org.jahia.support.modules.as.cli;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jahia.modules.augmentedsearch.graphql.AugmentedSearchException;
import org.jahia.modules.augmentedsearch.service.ESService;
import org.jahia.modules.augmentedsearch.settings.ESSettingsService;
import org.quartz.JobDetail;

@Command(scope = "as", name = "index", description = "Start indexing the platform")
@Service
public class StartIndexation implements Action {

    @Reference
    ESService esService;

    @Reference
    ESSettingsService esSettingsService;

    @Option(name = "--force", description = "Force indexation", required = false)
    private boolean forceIndexation;

    @Override
    public Object execute() throws Exception {
        if (esSettingsService.getAvailableConnectorsNames().isEmpty()) {
            throw new AugmentedSearchException("No connection is configured, you can pick one in Augmented Search Manager");
        }
        // Reset shard/replicas templates if needed
        esSettingsService.setUpIngestionPipelines();
        esSettingsService.setUpIndexTemplates();
        JobDetail jobDetail = esService.fullReIndexUsingJob(null, forceIndexation);
        return jobDetail.getFullName();
    }

}
