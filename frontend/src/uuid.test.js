import {describe,it,expect,vi,afterEach} from 'vitest';
import {webcrypto} from 'node:crypto';
import {createUuid} from './uuid';
import {guest} from './api';
afterEach(()=>vi.unstubAllGlobals());
describe('secure LAN UUID support',()=>{
 it('uses the native UUID method when available',()=>{const native=vi.fn(()=> 'native-uuid');expect(createUuid({randomUUID:native})).toBe('native-uuid');expect(native).toHaveBeenCalledOnce();});
 it('sets version and variant bits using random bytes when randomUUID is missing',()=>{expect(createUuid({getRandomValues:bytes=>bytes.fill(255)})).toBe('ffffffff-ffff-4fff-bfff-ffffffffffff');});
 it('generates distinct valid UUIDs using the secure fallback',()=>{const cryptoApi={getRandomValues:bytes=>webcrypto.getRandomValues(bytes)};const values=Array.from({length:100},()=>createUuid(cryptoApi));expect(new Set(values).size).toBe(100);values.forEach(v=>expect(v).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/));});
 it('fails safely if cryptographic randomness is unavailable',()=>{expect(()=>createUuid({})).toThrow('Secure random generation is unavailable');});
 it('creates and preserves a guest session without randomUUID',()=>{const data=new Map();vi.stubGlobal('crypto',{getRandomValues:bytes=>webcrypto.getRandomValues(bytes)});vi.stubGlobal('localStorage',{getItem:k=>data.get(k)||null,setItem:(k,v)=>data.set(k,v)});const first=guest();expect(guest()).toBe(first);expect(first).toHaveLength(36);});
});
