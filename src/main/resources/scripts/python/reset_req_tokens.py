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

def login_and_get_request_token(creds, request_token_url):
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

        # Step 3: Get request token
        token_response = session.get(request_token_url)
        token_response.raise_for_status()
        print("Login and request token renewal successful.")
        print("Request Token Response:", token_response.text)
        return token_response.text

    except requests.exceptions.RequestException as e:
        print(f"An error occurred during the request: {e}")
    except KeyError as e:
        print(f"Unexpected response structure: Missing key {e}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Retrieve request token from Kite platform.")

    parser.add_argument('--name', required=False, help="Name of the user.")
    parser.add_argument('--user_id', required=True, help="User ID of the user.")
    parser.add_argument('--password', required=True, help="Password of the user.")
    parser.add_argument('--totp_key', required=True, help="TOTP Key for a user.")
    parser.add_argument('--api_key', required=True, help="API Key of the user.")
    parser.add_argument('--api_secret', required=True, help="API Secret of the user.")
    parser.add_argument('--request_token_url', required=True, help="Request token URL.")

    args = parser.parse_args()

    # Credentials
    creds = {
        'name': args.name,
        'user_id': args.user_id,
        'password': args.password,
        'totp_key': args.totp_key,
        'api_key': args.api_key,
        'api_secret': args.api_secret
    }

    #Format request url
    request_token_url = ( args.request_token_url.format(
        apiKey=creds['api_key'],
        userId=creds['user_id'],
        name=creds['name']
    )
    )
    # Call the function with the provided arguments
    login_and_get_request_token(creds, request_token_url)

