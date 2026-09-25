/**
 * WATZEM Monetization & Support Configuration
 * Community Support & Crypto Donations
 * 
 * Replace the placeholder addresses and links below with your real wallet addresses.
 */

export interface CryptoWallet {
  id: string;
  name: string;
  symbol: string;
  network: string;
  address: string;
  memo?: string;
  badgeColor?: string;
}

export const MONETIZATION_CONFIG = {
  // Support & Donation Links
  coffee: {
    enabled: true,
    name: 'Buy Me a Coffee',
    url: (import.meta.env.VITE_BUYMEACOFFEE_URL as string) || 'https://buymeacoffee.com/watzem',
  },

  // Crypto Donation Wallets (One-tap copy to clipboard)
  cryptoWallets: [
    {
      id: 'btc',
      name: 'Bitcoin',
      symbol: 'BTC',
      network: 'Bitcoin Native SegWit',
      address: 'bc1qqmpyx5k3y6f9xy3xgkxgkg4ndjmv9dcqckjaqu',
      badgeColor: '#f7931a'
    },
    {
      id: 'sol',
      name: 'Solana',
      symbol: 'SOL',
      network: 'Solana Network',
      address: '4UCD4UdjPDQgAxtJGG78Ytz7nxAwXvUPmUWVdzDC31Q6',
      badgeColor: '#14f195'
    },
    {
      id: 'eth',
      name: 'Ethereum',
      symbol: 'ETH',
      network: 'Ethereum (ERC-20)',
      address: '0xd173F57184765096e13B2Ccdb9e0A111acF9cc0b',
      badgeColor: '#627eea'
    }
  ] as CryptoWallet[]
};
