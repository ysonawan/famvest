import pyotp
import argparse
import sys

def get_totp(totp_key):
    """
    Generates the current TOTP PIN from the provided TOTP key.

    Args:
        totp_key: The base32 encoded TOTP key

    Returns:
        The current 6-digit TOTP PIN as a string
    """
    try:
        totp = pyotp.TOTP(totp_key)
        return totp.now()
    except Exception as e:
        print(f"An error occurred while generating TOTP: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate TOTP PIN from TOTP key.")
    parser.add_argument('--totp_key', required=True, help="TOTP Key (base32 encoded).")

    args = parser.parse_args()

    totp_pin = get_totp(args.totp_key)
    print(totp_pin)

