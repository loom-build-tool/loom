package jobt.plugin.checkstyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

public class LoggingAuditListener implements AuditListener {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingAuditListener.class);

    @Override
    public void auditStarted(final AuditEvent event) {
        LOG.info("Starting audit");
    }

    @Override
    public void auditFinished(final AuditEvent event) {
        LOG.info("Audit finished");
    }

    @Override
    public void fileStarted(final AuditEvent event) {
        LOG.debug("Start file {}", event.getFileName());
    }

    @Override
    public void fileFinished(final AuditEvent event) {
        LOG.debug("Finished file {}", event.getFileName());
    }

    @Override
    public void addError(final AuditEvent event) {
        final SeverityLevel severityLevel = event.getSeverityLevel();
        final String msg = "{}:{}:{} {} [{}]";
        switch (severityLevel) {
            case ERROR:
                LOG.error(msg, event.getFileName(), event.getLine(), event.getColumn(),
                    event.getMessage(), event.getSourceName());
                break;
            case WARNING:
                LOG.warn(msg, event.getFileName(), event.getLine(), event.getColumn(),
                    event.getMessage(), event.getSourceName());
                break;
            case INFO:
                LOG.info(msg, event.getFileName(), event.getLine(), event.getColumn(),
                    event.getMessage(), event.getSourceName());
                break;
            default:
                LOG.debug(msg, event.getFileName(), event.getLine(), event.getColumn(),
                    event.getMessage(), event.getSourceName());
                break;
        }
    }

    @Override
    public void addException(final AuditEvent event, final Throwable throwable) {
        LOG.error("Error processing file {}", event.getFileName(), throwable);
    }

}
