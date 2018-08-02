package pl.raaadziu.nb1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
class SimpleWorker
{
    private MemBox memBox;

    @Autowired
    SimpleWorker(MemBox memBox)
    {
        this.memBox = memBox;
    }

    @Scheduled(fixedRate = 15000)
    public void refreshTimer() throws  SqlApiException
    {
        memBox.Refresh();
    }

}
