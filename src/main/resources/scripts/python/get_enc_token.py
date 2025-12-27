import os
import json
import time
import requests
import pyotp
import argparse
import datetime as dt
import pandas as pd
import numpy as np

#apt install python3-pip
#apt install python3-pyotp
#apt install python3-pandas

BASE_URL = "https://kite.zerodha.com"
LOGIN_URL = f"{BASE_URL}/api/login"
TWOFA_URL = f"{BASE_URL}/api/twofa"

def login_and_get_enc_token(creds):
    """
    Logs in to the Kite platform and retrieves the request token.
    """
    try:
        session = requests.Session()

        # Step 1: Login
        login_response = session.post(
            LOGIN_URL,
            data={'user_id': creds['user_id'], 'password': creds['password']}
        )
        login_response.raise_for_status()
        login_data = login_response.json()
        request_id = login_data['data']['request_id']

        # Step 2: Two-factor authentication
        twofa_pin = pyotp.TOTP(creds['totp_key']).now()
        twofa_response = session.post(
            TWOFA_URL,
            data={
                'user_id': creds['user_id'],
                'request_id': request_id,
                'twofa_value': twofa_pin,
                'twofa_type': 'totp'
            }
        )
        twofa_response.raise_for_status()
        cookies_dict = twofa_response.cookies.get_dict();
        enc_token = cookies_dict.get('enctoken');
        return enc_token
    except requests.exceptions.RequestException as e:
        print(f"An error occurred during the request: {e}")
    except KeyError as e:
        print(f"Unexpected response structure: Missing key {e}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Retrieve enc token from Kite platform.")

    parser.add_argument('--user_id', required=True, help="User ID of the user.")
    parser.add_argument('--password', required=True, help="Password of the user.")
    parser.add_argument('--totp_key', required=True, help="TOTP Key for a user.")

    args = parser.parse_args()

    # Credentials
    creds = {
        'user_id': args.user_id,
        'password': args.password,
        'totp_key': args.totp_key,
    }

    # Call the function with the provided arguments
    # Call the function and print the returned value
    enc_token = login_and_get_enc_token(creds)
    print(enc_token)
