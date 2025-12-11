package currency.api;

import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;
import net.sharksystem.SharkPeer;
import net.sharksystem.pki.SharkPKIComponent;
import testhelpers.SharkCurrencyComponentImpl;

/**
 * Factory of the currency component bounding it to the SharkPki
 */
public class SharkCurrencyComponentFactory implements SharkComponentFactory {
    private final SharkPKIComponent pkiComponent;

    public SharkCurrencyComponentFactory(SharkPKIComponent pkiComponent) {
        this.pkiComponent = pkiComponent;
    }

    @Override
    public SharkComponent getComponent(SharkPeer sharkPeer) {
        return new SharkCurrencyComponentImpl(pkiComponent);
    }
}
