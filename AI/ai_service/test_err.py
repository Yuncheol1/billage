import asyncio
import traceback
from app.services.analyzer import AnalyzeService

async def main():
    try:
        s = AnalyzeService()
        result = await s.analyze_document('hello', 'full', 10000000, 0)
        print("Success:", result)
    except Exception as e:
        print("Exception caught:")
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())
