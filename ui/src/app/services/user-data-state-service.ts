import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UserDataStateService {

  constructor() { }

  private readonly key = 'userDataState';

  getState(): any {
    const state = localStorage.getItem(this.key);
    if (state) {
      return JSON.parse(state);
    } else {
      return JSON.parse('{}'); // Return an empty object if no state is found
    }
  }

  setState(partialState: Partial<any>): void {
    const currentState = this.getState();
    const newState = { ...currentState, ...partialState };
    localStorage.setItem(this.key, JSON.stringify(newState));
  }

  clearState(): void {
    localStorage.removeItem(this.key);
  }
}
